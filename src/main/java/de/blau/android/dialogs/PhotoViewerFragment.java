package de.blau.android.dialogs;

import java.util.ArrayList;
import java.util.List;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.app.Dialog;
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
import android.widget.FrameLayout;
import de.blau.android.R;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.util.ImmersiveDialogFragment;
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

    /**
     * Show an info dialog for the supplied OsmElement
     * 
     * @param activity the calling Activity
     * @param photoList
     * @param startPos
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
     * @param photoList
     * @param startPos
     * @return
     */
    public static PhotoViewerFragment newInstance(@NonNull ArrayList<String> photoList, int startPos) {
        PhotoViewerFragment f = new PhotoViewerFragment();

        Bundle args = new Bundle();
        args.putStringArrayList(PHOTO_LIST_KEY, photoList);
        args.putInt(START_POS_KEY, startPos);

        f.setArguments(args);
        // f.setShowsDialog(true);

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
        List<String> photoList = getArguments().getStringArrayList(PHOTO_LIST_KEY);
        int startPos = getArguments().getInt(START_POS_KEY);
        View layout = themedInflater.inflate(R.layout.photo_viewer, null);
        SubsamplingScaleImageView photoView = layout.findViewById(R.id.photoView);
        photoView.setOrientation(SubsamplingScaleImageView.ORIENTATION_90);
        photoView.setImage(ImageSource.uri(photoList.get(startPos)));
        Toolbar toolbar = (Toolbar) layout.findViewById(R.id.photoToolbar);
        Menu menu = toolbar.getMenu();
        menu.add(R.string.delete).setIcon(R.drawable.ic_action_discard_holo_dark).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
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
    public boolean onMenuItemClick(MenuItem arg0) {
        System.out.println("menu pressed");
        return false;
    }
}
