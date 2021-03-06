package nl.changer.polypicker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import nl.changer.polypicker.model.Image;
import nl.changer.polypicker.utils.ImageInternalFetcher;
import nl.changer.polypicker.utils.Utils;

public class ImagePickerActivity extends AppCompatActivity {

    /**
     * Key to persist the list when saving the state of the activity.
     */
    private static final String KEY_LIST = "nl.changer.polypicker.savedinstance.key.list";

    /**
     * Returns the parcelled image uris in the intent with this extra.
     */
    public static final String EXTRA_IMAGE_URIS = "nl.changer.changer.nl.polypicker.extra.selected_image_uris";
    public static final String EXTRA_IMAGES_TO_DISPLAY = "nl.changer.changer.nl.polypicker.extra.images_to_display";

    private LinkedHashSet<Image> mSelectedImages;
    private LinearLayout mSelectedImagesContainer;
    protected TextView mSelectedImageEmptyMessage;

    private ViewPager mViewPager;
    public ImageInternalFetcher mImageFetcher;

    private Button mCancelButtonView, mDoneButtonView;

    private SlidingTabText mSlidingTabText;

    // initialize with default config.
    private static Config mConfig = new Config.Builder().build();

    public static void setConfig(Config config) {

        if (config == null) {
            throw new NullPointerException("Config cannot be passed null. Not setting config will use default values.");
        }

        mConfig = config;
    }

    public static Config getConfig() {
        return mConfig;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pp__activity_main);

       /*
       // Dont enable the toolbar.
       // Consumes a lot of space in the UI unnecessarily.
       Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }*/

        mSelectedImagesContainer = (LinearLayout) findViewById(R.id.pp__selected_photos_container);
        mSelectedImageEmptyMessage = (TextView) findViewById(R.id.pp__selected_photos_empty);
        mViewPager = (ViewPager) findViewById(R.id.pp__pager);
        mCancelButtonView = (Button) findViewById(R.id.pp__btn_cancel);
        mDoneButtonView = (Button) findViewById(R.id.pp__btn_done);

        mSelectedImages = new LinkedHashSet<>();
        mImageFetcher = new ImageInternalFetcher(this, 500);

        mCancelButtonView.setOnClickListener(mOnFinishGettingImages);
        mDoneButtonView.setOnClickListener(mOnFinishGettingImages);

        setupActionBar();
        if (savedInstanceState != null) {
            populateUi(savedInstanceState);
        }

        if (getIntent().getExtras() != null){
            // There are pictures to (re)display
            populateUI(getIntent());
        }
    }

    protected void populateUi(Bundle savedInstanceState) {
        ArrayList<Image> list = savedInstanceState.getParcelableArrayList(KEY_LIST);
        displayImagesList(list);
    }

    protected void populateUI(Intent i){
        ArrayList<Image> list = i.getParcelableArrayListExtra(EXTRA_IMAGES_TO_DISPLAY);
        displayImagesList(list);
    }

    private void displayImagesList(ArrayList<Image> list) {
        if (list != null) {
            for (Image image : list) {
                addImage(image);
            }
        }
    }

    /**
     * Sets up the action bar, adding view page indicator.
     */
    protected void setupActionBar() {
        mSlidingTabText = (SlidingTabText) findViewById(R.id.pp__sliding_tabs);
        mSlidingTabText.setSelectedIndicatorColors(getResources().getColor(mConfig.getTabSelectionIndicatorColor()));
        mSlidingTabText.setCustomTabView(R.layout.pp__tab_view_text, R.id.pp_tab_text);
        mSlidingTabText.setTabStripColor(mConfig.getTabBackgroundColor());
        mViewPager.setAdapter(new PagerAdapter2Fragments(getFragmentManager()));
        mSlidingTabText.setTabTitles(getResources().getStringArray(R.array.tab_titles));
        mSlidingTabText.setViewPager(mViewPager);
    }

    public boolean addImage(Image image) {

        if (mSelectedImages == null) {
            // this condition may arise when the activity is being
            // restored when sufficient memory is available. onRestoreState()
            // will be called.
            mSelectedImages = new LinkedHashSet<>();
        }

        if (mSelectedImages.size() == mConfig.getSelectionLimit()) {
            Toast.makeText(this, getString(R.string.n_images_selected, mConfig.getSelectionLimit()), Toast.LENGTH_SHORT).show();
            return false;
        } else {
            if (mSelectedImages.add(image)) {
                DeletableImageView rootView = new DeletableImageView(this, image, new DeletableImageView.DeleteListener() {
                    @Override
                    public void onDelete(Image i) {
                        removeImage(i);
                    }
                });
                if(image.isWeb == 0) {
                    mImageFetcher.loadImage(image.mUri, rootView.getImagePreviewView());
                }
                else{
                    Glide.with(this).load(image.mUrl).into(rootView.getImagePreviewView());
                }
                mSelectedImagesContainer.addView(rootView);

                if (mSelectedImages.size() >= 1) {
                    mSelectedImagesContainer.setVisibility(View.VISIBLE);
                    mSelectedImageEmptyMessage.setVisibility(View.GONE);
                }
                return true;
            }
        }

        return false;
    }

    public boolean removeImage(Image image) {
        if (mSelectedImages.remove(image)) {
            for (int i = 0; i < mSelectedImagesContainer.getChildCount(); i++) {
                View childView = mSelectedImagesContainer.getChildAt(i);
                if (childView.getTag().equals(image)) {
                    mSelectedImagesContainer.removeViewAt(i);
                    break;
                }
            }

            if (mSelectedImages.size() == 0) {
                mSelectedImagesContainer.setVisibility(View.GONE);
                mSelectedImageEmptyMessage.setVisibility(View.VISIBLE);
            }
            Intent refreshIntent = new Intent();
            refreshIntent.setAction(Utils.Events.SELECTION_CHANGED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(refreshIntent);
            return true;
        }
        return false;
    }

    public boolean containsImage(Image image) {
        return mSelectedImages.contains(image);
    }

    private View.OnClickListener mOnFinishGettingImages = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.pp__btn_done) {

                Image[] images = new Image[mSelectedImages.size()];
                int i = 0;
                for (Image img : mSelectedImages) {
                    images[i++] = img;
                }

                Intent intent = new Intent();
                intent.putExtra(EXTRA_IMAGE_URIS, images);
                setResult(Activity.RESULT_OK, intent);
            } else if (view.getId() == R.id.pp__btn_cancel) {
                setResult(Activity.RESULT_CANCELED);
            }
            finish();
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // preserve already taken images on configuration changes like
        // screen rotation or activity run out of memory.
        // HashSet cannot be saved, so convert to list and then save.
        ArrayList<Image> list = new ArrayList<Image>(mSelectedImages);
        outState.putParcelableArrayList(KEY_LIST, list);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        populateUi(savedInstanceState);
    }
}