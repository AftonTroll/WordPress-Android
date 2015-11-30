package org.wordpress.android.editor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spannable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.helpers.MediaFile;
import org.wordpress.android.util.helpers.WPImageSpan;

public class LegacySettingsHelper {

    private LegacySettingsHelper() {

    }

//=====================================================================
// IMAGE
//=====================================================================

    public static void showImageSettings(final View alertView, final EditText titleText,
                                         final EditText caption, final EditText imageWidthText,
                                         final CheckBox featuredCheckBox, final CheckBox featuredInPostCheckBox,
                                         final int maxWidth, final Spinner alignmentSpinner, final WPImageSpan imageSpan,
                                         final LegacyEditorFragment fragment, final EditText mContentEditText) {
        Log.w("AFTON", "LEGACY show image settings");
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(fragment.getString(R.string.image_settings));
        builder.setView(alertView);
        builder.setPositiveButton(fragment.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = (titleText.getText() != null) ? titleText.getText().toString() : "";
                MediaFile mediaFile = imageSpan.getMediaFile();
                if (mediaFile == null) {
                    return;
                }
                mediaFile.setTitle(title);
                mediaFile.setHorizontalAlignment(alignmentSpinner.getSelectedItemPosition());
                mediaFile.setWidth(fragment.getEditTextIntegerClamped(imageWidthText, 10, maxWidth));
                String captionText = (caption.getText() != null) ? caption.getText().toString() : "";
                mediaFile.setCaption(captionText);
                mediaFile.setFeatured(featuredCheckBox.isChecked());
                if (featuredCheckBox.isChecked()) {
                    // remove featured flag from all other images
                    Spannable contentSpannable = mContentEditText.getText();
                    WPImageSpan[] imageSpans =
                            contentSpannable.getSpans(0, contentSpannable.length(), WPImageSpan.class);
                    if (imageSpans.length > 1) {
                        for (WPImageSpan postImageSpan : imageSpans) {
                            if (postImageSpan != imageSpan) {
                                MediaFile postMediaFile = postImageSpan.getMediaFile();
                                postMediaFile.setFeatured(false);
                                postMediaFile.setFeaturedInPost(false);
                                // TODO: remove this
                                fragment.mEditorFragmentListener.saveMediaFile(postMediaFile);
                            }
                        }
                    }
                }
                mediaFile.setFeaturedInPost(featuredInPostCheckBox.isChecked());
                // TODO: remove this
                fragment.mEditorFragmentListener.saveMediaFile(mediaFile);
            }
        });
        builder.setNegativeButton(fragment.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        alertDialog.show();
    }


    public static boolean inflateImage(MediaFile mediaFile, WPImageSpan imageSpan, final LegacyEditorFragment fragment, final EditText mContentEditText) {
        LayoutInflater factory = LayoutInflater.from(fragment.getActivity());
        final View alertView = factory.inflate(R.layout.alert_image_options, null);
        if (alertView == null)
            return false;
        final EditText imageWidthText = (EditText) alertView.findViewById(R.id.imageWidthText);
        final EditText titleText = (EditText) alertView.findViewById(R.id.title);
        final EditText caption = (EditText) alertView.findViewById(R.id.caption);
        final CheckBox featuredCheckBox = (CheckBox) alertView.findViewById(R.id.featuredImage);
        final CheckBox featuredInPostCheckBox = (CheckBox) alertView.findViewById(R.id.featuredInPost);

        // show featured image checkboxes if supported
        if (fragment.mFeaturedImageSupported) {
            featuredCheckBox.setVisibility(View.VISIBLE);
            featuredInPostCheckBox.setVisibility(View.VISIBLE);
        }

        featuredCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    featuredInPostCheckBox.setVisibility(View.VISIBLE);
                } else {
                    featuredInPostCheckBox.setVisibility(View.GONE);
                }

            }
        });

        final SeekBar seekBar = (SeekBar) alertView.findViewById(R.id.imageWidth);
        final Spinner alignmentSpinner = (Spinner) alertView.findViewById(R.id.alignment_spinner);
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(fragment.getActivity(), R.array.alignment_array,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alignmentSpinner.setAdapter(adapter);

        imageWidthText.setText(String.valueOf(mediaFile.getWidth()) + "px");
        seekBar.setProgress(mediaFile.getWidth());
        titleText.setText(mediaFile.getTitle());
        caption.setText(mediaFile.getCaption());
        featuredCheckBox.setChecked(mediaFile.isFeatured());

        if (mediaFile.isFeatured()) {
            featuredInPostCheckBox.setVisibility(View.VISIBLE);
        } else {
            featuredInPostCheckBox.setVisibility(View.GONE);
        }

        featuredInPostCheckBox.setChecked(mediaFile.isFeaturedInPost());

        alignmentSpinner.setSelection(mediaFile.getHorizontalAlignment(), true);

        final int maxWidth = MediaUtils.getMinimumImageWidth(fragment.getActivity(),
                imageSpan.getImageSource(), fragment.mBlogSettingMaxImageWidth);
        seekBar.setMax(maxWidth / 10);
        if (mediaFile.getWidth() != 0) {
            seekBar.setProgress(mediaFile.getWidth() / 10);
        }
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    progress = 1;
                }
                imageWidthText.setText(progress * 10 + "px");
            }
        });

        imageWidthText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    imageWidthText.setText("");
                }
            }
        });

        imageWidthText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                int width = fragment.getEditTextIntegerClamped(imageWidthText, 10, maxWidth);
                seekBar.setProgress(width / 10);
                imageWidthText.setSelection((String.valueOf(width).length()));

                InputMethodManager imm = (InputMethodManager) fragment.getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(imageWidthText.getWindowToken(),
                        InputMethodManager.RESULT_UNCHANGED_SHOWN);

                return true;
            }
        });

        LegacySettingsHelper.showImageSettings(alertView, titleText, caption, imageWidthText, featuredCheckBox,
                featuredInPostCheckBox, maxWidth, alignmentSpinner, imageSpan,
                fragment, mContentEditText);
        fragment.mScrollDetected = false;
        return true;
    }
//=====================================================================
// VIDEO
//=====================================================================
    public static void showVideoSettings(final View alertView, final EditText titleText,
                                         final EditText caption,
                                         final CheckBox featuredCheckBox, final CheckBox featuredInPostCheckBox, final Spinner alignmentSpinner, final WPImageSpan imageSpan,
                                         final LegacyEditorFragment fragment, final EditText mContentEditText) {
        Log.w("AFTON", "LEGACY show image settings");
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
        builder.setTitle(fragment.getString(R.string.image_settings));
        builder.setView(alertView);
        builder.setPositiveButton(fragment.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String title = (titleText.getText() != null) ? titleText.getText().toString() : "";
                MediaFile mediaFile = imageSpan.getMediaFile();
                if (mediaFile == null) {
                    return;
                }
                mediaFile.setTitle(title);
                mediaFile.setHorizontalAlignment(alignmentSpinner.getSelectedItemPosition());
                String captionText = (caption.getText() != null) ? caption.getText().toString() : "";
                mediaFile.setCaption(captionText);
                mediaFile.setFeatured(featuredCheckBox.isChecked());
                if (featuredCheckBox.isChecked()) {
                    // remove featured flag from all other images
                    Spannable contentSpannable = mContentEditText.getText();
                    WPImageSpan[] imageSpans =
                            contentSpannable.getSpans(0, contentSpannable.length(), WPImageSpan.class);
                    if (imageSpans.length > 1) {
                        for (WPImageSpan postImageSpan : imageSpans) {
                            if (postImageSpan != imageSpan) {
                                MediaFile postMediaFile = postImageSpan.getMediaFile();
                                postMediaFile.setFeatured(false);
                                postMediaFile.setFeaturedInPost(false);
                                // TODO: remove this
                                fragment.mEditorFragmentListener.saveMediaFile(postMediaFile);
                            }
                        }
                    }
                }
                mediaFile.setFeaturedInPost(featuredInPostCheckBox.isChecked());
                // TODO: remove this
                fragment.mEditorFragmentListener.saveMediaFile(mediaFile);
            }
        });
        builder.setNegativeButton(fragment.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        alertDialog.show();
    }


    public static boolean inflateVideo(MediaFile mediaFile, WPImageSpan imageSpan, final LegacyEditorFragment fragment, final EditText mContentEditText) {
        LayoutInflater factory = LayoutInflater.from(fragment.getActivity());
        final View alertView = factory.inflate(R.layout.alert_image_options, null);
        if (alertView == null)
            return false;
        final EditText titleText = (EditText) alertView.findViewById(R.id.title);
        final EditText caption = (EditText) alertView.findViewById(R.id.caption);
        final CheckBox featuredCheckBox = (CheckBox) alertView.findViewById(R.id.featuredImage);
        final CheckBox featuredInPostCheckBox = (CheckBox) alertView.findViewById(R.id.featuredInPost);
        final TextView widthText = (TextView) alertView.findViewById(R.id.image_width_header);
        final SeekBar seekBar = (SeekBar) alertView.findViewById(R.id.imageWidth);

        seekBar.setVisibility(View.GONE);
        widthText.setVisibility(View.GONE);

        // show featured image checkboxes if supported
        if (fragment.mFeaturedImageSupported) {
            featuredCheckBox.setVisibility(View.VISIBLE);
            featuredInPostCheckBox.setVisibility(View.VISIBLE);
        }

        featuredCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    featuredInPostCheckBox.setVisibility(View.VISIBLE);
                } else {
                    featuredInPostCheckBox.setVisibility(View.GONE);
                }

            }
        });

        final Spinner alignmentSpinner = (Spinner) alertView.findViewById(R.id.alignment_spinner);
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(fragment.getActivity(), R.array.alignment_array,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alignmentSpinner.setAdapter(adapter);

        titleText.setText(mediaFile.getTitle());
        caption.setText(mediaFile.getCaption());
        featuredCheckBox.setChecked(mediaFile.isFeatured());

        if (mediaFile.isFeatured()) {
            featuredInPostCheckBox.setVisibility(View.VISIBLE);
        } else {
            featuredInPostCheckBox.setVisibility(View.GONE);
        }

        featuredInPostCheckBox.setChecked(mediaFile.isFeaturedInPost());

        alignmentSpinner.setSelection(mediaFile.getHorizontalAlignment(), true);

        LegacySettingsHelper.showVideoSettings(alertView, titleText, caption, featuredCheckBox,
                featuredInPostCheckBox, alignmentSpinner, imageSpan, fragment, mContentEditText);
        fragment.mScrollDetected = false;
        return true;
    }
}
