package nl.changer.polypicker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;

import nl.changer.polypicker.model.Image;
import nl.changer.polypicker.utils.BroadcastFragment;
import nl.changer.polypicker.utils.Utils;


/**
 * Created by Gil on 04/03/2014.
 */
public class GalleryFragment extends BroadcastFragment {

    private static final String TAG = GalleryFragment.class.getSimpleName();

    private ImageGalleryAdapter mGalleryAdapter;
    private ImagePickerActivity mActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.pp__fragment_gallery, container, false);

        mGalleryAdapter = new ImageGalleryAdapter(getActivity());
        GridView galleryGridView = (GridView) rootView.findViewById(R.id.pp__gallery_grid);
        mActivity = ((ImagePickerActivity) getActivity());

        Cursor imageCursor = null;
        try {
        	final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.ImageColumns.ORIENTATION};
            final String orderBy = MediaStore.Images.Media.DATE_ADDED + " DESC";
            imageCursor = getActivity().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null, null, orderBy);
            while (imageCursor.moveToNext()) {
                Uri uri = Uri.parse(imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA)));
                int orientation = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION));
                mGalleryAdapter.add(new Image(uri, orientation));
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(imageCursor != null && !imageCursor.isClosed()) {
				imageCursor.close();	
			}	
		}

        galleryGridView.setAdapter(mGalleryAdapter);
        galleryGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Image image = mGalleryAdapter.getItem(i);
                if (!mActivity.containsImage(image)) {
                    mActivity.addImage(image);
                } else {
                    mActivity.removeImage(image);
                }

                // refresh the view to
                // mGalleryAdapter.getView(i, view, adapterView);
                mGalleryAdapter.notifyDataSetChanged();
            }
        });

        return rootView;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public BroadcastReceiver setupBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Utils.Events.SELECTION_CHANGED)){
                    mGalleryAdapter.notifyDataSetChanged();
                }
            }
        };
    }

    @Override
    public IntentFilter setupIntentFilter() {
        IntentFilter inf = new IntentFilter();
        inf.addAction(Utils.Events.SELECTION_CHANGED);
        return inf;
    }

    class ViewHolder {
        ImageView mThumbnail;
        // This is like storing too much data in memory.
        // find a better way to handle this
        Image mImage;
    }

    public class ImageGalleryAdapter extends ArrayAdapter<Image> {

        public ImageGalleryAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.pp__grid_item_gallery_thumbnail, null);
                holder = new ViewHolder();
                holder.mThumbnail = (ImageView) convertView.findViewById(R.id.pp__thumbnail_image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Image image = getItem(position);
            boolean isSelected = mActivity.containsImage(image);

            ((FrameLayout) convertView).setForeground(isSelected ? getResources().getDrawable(R.drawable.gallery_photo_selected) : null);

            if (holder.mImage == null || !holder.mImage.equals(image)) {
                mActivity.mImageFetcher.loadImage(image.mUri, holder.mThumbnail);
                holder.mImage = image;
            }
            return convertView;
        }
    }
}