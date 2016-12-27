package nl.changer.polypicker.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Gil on 04/03/2014.
 */
public class Image implements Parcelable {

    public Uri mUri;
    public int mOrientation;
    public int isWeb;
    public String mUrl;
    public int mId = -1;

    public Image(Uri uri, int orientation) {
        isWeb = 0;
        mUri = uri;
        mOrientation = orientation;
    }

    /**
     * This constructor is used to get the new order of an already uploaded picture.
     * @param url
     * @param id
     */
    public Image(String url, int id){
        isWeb = 1;
        mUrl = url;
        mId = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mUri, 0);
        dest.writeInt(this.mOrientation);
        dest.writeInt(this.isWeb);
        dest.writeString(this.mUrl);
        dest.writeInt(this.mId);
    }

    private Image(Parcel in) {
        this.mUri = in.readParcelable(Uri.class.getClassLoader());
        this.mOrientation = in.readInt();
        this.isWeb = in.readInt();
        this.mUrl = in.readString();
        this.mId = in.readInt();
    }

    public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {
        public Image createFromParcel(Parcel source) {
            return new Image(source);
        }

        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Image image = (Image) o;

        if (isWeb != image.isWeb) return false;
        if (mId != image.mId) return false;
        if (mUri != null ? !mUri.equals(image.mUri) : image.mUri != null) return false;
        return mUrl != null ? mUrl.equals(image.mUrl) : image.mUrl == null;

    }

    @Override
    public int hashCode() {
        int result = mUri != null ? mUri.hashCode() : 0;
        result = 31 * result + isWeb;
        result = 31 * result + (mUrl != null ? mUrl.hashCode() : 0);
        result = 31 * result + mId;
        return result;
    }
}
