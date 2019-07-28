package de.blau.android.dialogs;

import java.util.ArrayList;
import java.util.List;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.photos.PhotoIndex;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

/**
 * Very simple dialog fragment to display some info on an OsmElement and potentially compare it with an UndoElement
 * 
 * @author simon
 *
 */
public class PhotoViewerFragment extends ImmersiveDialogFragment implements Toolbar.OnMenuItemClickListener {
    private static final String DEBUG_TAG = PhotoViewerFragment.class.getName();

    public static final String TAG = "fragment_photo_viewer";

    private static final String PHOTO_LIST_KEY = "photo_list";
    private static final String START_POS_KEY  = "start_pos";

    private static final int MENUITEM_SHARE  = 0;
    private static final int MENUITEM_DELETE = 1;

    private List<String> photoList = null;
    private int          startPos  = 0;
    private int          pos       = 0;

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param photoList list of Uris
     * @param startPos starting position in the list
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull ArrayList<String> photoList, int startPos) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            PhotoViewerFragment photoViewerFragment = newInstance(photoList, startPos);
            photoViewerFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * 
     * @param photoList list of Uris
     * @param startPos starting position in the list
     * @return a n new instance of PhotoViwerFragment
     */
    @NonNull
    public static PhotoViewerFragment newInstance(@NonNull ArrayList<String> photoList, int startPos) {
        PhotoViewerFragment f = new PhotoViewerFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PHOTO_LIST_KEY, photoList);
        args.putInt(START_POS_KEY, startPos);

        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);
        builder.setView(createView(null));
        return builder.create();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!getShowsDialog()) {
            return createView(container);
        }
        return null;
    }

    /**
     * Create the view we want to display
     * 
     * @param container parent view or null
     * @return the View
     */
    private View createView(ViewGroup container) {
        FragmentActivity activity = getActivity();
        LayoutInflater themedInflater = ThemeUtils.getLayoutInflater(activity);
        photoList = getArguments().getStringArrayList(PHOTO_LIST_KEY);
        startPos = getArguments().getInt(START_POS_KEY);
        pos = startPos;
        View layout = themedInflater.inflate(R.layout.photo_viewer, null);
        SubsamplingScaleImageView photoView = layout.findViewById(R.id.photoView);
        photoView.setOrientation(SubsamplingScaleImageView.ORIENTATION_90);
        photoView.setImage(ImageSource.uri(photoList.get(startPos)));
        System.out.println(photoList.get(startPos));
        Toolbar toolbar = (Toolbar) layout.findViewById(R.id.photoToolbar);
        Menu menu = toolbar.getMenu();
        menu.add(Menu.NONE, MENUITEM_SHARE, Menu.NONE, R.string.share).setIcon(R.drawable.ic_share_white_36dp)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        if (Uri.parse(photoList.get(startPos)).getAuthority().equals(getString(R.string.content_provider))) {
            // we can only delete stuff that is provided by our provider
            menu.add(Menu.NONE, MENUITEM_DELETE, Menu.NONE, R.string.delete).setIcon(R.drawable.ic_action_discard_holo_dark)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        toolbar.setOnMenuItemClickListener(this);
        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (photoList != null && !photoList.isEmpty() && pos < photoList.size()) {
            switch (item.getItemId()) {
            case MENUITEM_SHARE:
                de.blau.android.layer.photos.Util.startExternalPhotoViewer(getContext(), Uri.parse(photoList.get(pos)));
                if (getShowsDialog() && photoList.size() == 1) {
                    getDialog().dismiss();
                }
                break;
            case MENUITEM_DELETE:
                new AlertDialog.Builder(getContext()).setTitle(R.string.photo_viewer_delete_title)
                        .setPositiveButton(R.string.photo_viewer_delete_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Uri photoUri = Uri.parse(photoList.get(pos));
                                try {
                                    // delete from inmemory and on device index
                                    PhotoIndex index = new PhotoIndex(getContext());
                                    index.deletePhoto(getContext(), photoUri);
                                    // actually delete
                                    if (getContext().getContentResolver().delete(photoUri, null, null) >= 1) {
                                        photoList.remove(pos);
                                        pos = Integer.min(pos, photoList.size() - 1); // this will set pos to -1 but we
                                                                                      // will exit in that case                                       
                                        if (getShowsDialog() && photoList.isEmpty()) { // in fragment mode we want
                                                                                       // to do something else
                                            getDialog().dismiss();
                                        }
                                    }
                                } catch (java.lang.SecurityException sex) {
                                    Snack.toastTopError(getContext(), getString(R.string.toast_permission_denied, sex.getMessage()));
                                }
                            }
                        }).setNeutralButton(R.string.cancel, null).show();

                break;
            default:
                // do nothing
            }
        }
        return false;
    }
}
