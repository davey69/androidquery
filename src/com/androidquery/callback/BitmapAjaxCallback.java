/*
 * Copyright 2011 - AndroidQuery.com (tinyeeliu@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.androidquery.callback;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.androidquery.AQuery;
import com.androidquery.util.AQUtility;
import com.androidquery.util.BitmapCache;
import com.androidquery.util.RatioDrawable;


/**
 * The callback handler for handling Aquery.image() methods.
 */
public class BitmapAjaxCallback extends AbstractAjaxCallback<Bitmap, BitmapAjaxCallback>{

	private static final String TAG = BitmapAjaxCallback.class.getSimpleName();
	private static int SMALL_MAX = 20;
	private static int BIG_MAX = 20;
	private static int SMALL_PIXELS = 50 * 50;
	private static int BIG_PIXELS = 400 * 400;
	private static int BIG_TPIXELS = 1000000;
	
	private static Map<String, Bitmap> smallCache;
	private static Map<String, Bitmap> bigCache;
	private static Map<String, Bitmap> invalidCache;
	
	private static HashMap<String, WeakHashMap<ImageView, BitmapAjaxCallback>> queueMap = new HashMap<String, WeakHashMap<ImageView, BitmapAjaxCallback>>();	
	
	private WeakReference<ImageView> v;
	private int targetWidth;
	private int fallback;
	private File imageFile;
	private Bitmap bm;
	private int animation;
	private Bitmap preset;
	private float ratio;
	private boolean targetDim = true;
	private float anchor = AQuery.ANCHOR_DYNAMIC;
	private boolean invalid;
	private Options reuse;
	
	
	/**
	 * Instantiates a new bitmap ajax callback.
	 */
	public BitmapAjaxCallback(){
		type(Bitmap.class).memCache(true).fileCache(true).url("");
	}
	
	
	
	/**
	 * Set the target Image view.
	 *
	 * @param view the view
	 * @return self
	 */	
	public BitmapAjaxCallback imageView(ImageView view){				
		v = new WeakReference<ImageView>(view);		
		return this;
	}
	
	/**
	 * Set the target width for downsampling.
	 *
	 * @param targetWidth the target width
	 * @return self
	 */
	public BitmapAjaxCallback targetWidth(int targetWidth){
		this.targetWidth = targetWidth;
		return this;
	}
	
	
	/**
	 * Set the image source file.
	 *
	 * @param imageFile the image file
	 * @return self
	 */
	public BitmapAjaxCallback file(File imageFile){
		this.imageFile = imageFile;
		return this;
	}
	
	/**
	 * Set the preset bitmap. This bitmap will be shown immediately until the ajax callback returns the final image from the url.
	 *
	 * @param preset the preset
	 * @return self
	 */
	public BitmapAjaxCallback preset(Bitmap preset){
		
		this.preset = preset;
		return this;
	}
	
	/**
	 * Set the bitmap. This bitmap will be shown immediately with aspect ratio. 
	 *
	 * @param bm 
	 * @return self
	 */
	public BitmapAjaxCallback bitmap(Bitmap bm){
		this.bm = bm;
		return this;
	}
	
	/**
	 * Set the fallback image in resource id.
	 *
	 * @param resId the res id
	 * @return self
	 */
	public BitmapAjaxCallback fallback(int resId){
		this.fallback = resId;
		return this;
	}
	
	/**
	 * Set the animation resource id, or AQuery.FADE_IN.
	 *
	 * @param animation the animation
	 * @return self
	 */
	public BitmapAjaxCallback animation(int animation){
		this.animation = animation;
		return this;
	}
	
	/**
	 * Set the image aspect ratio (height / width).
	 *
	 * @param ratio the ratio
	 * @return self
	 */
	public BitmapAjaxCallback ratio(float ratio){
		this.ratio = ratio;
		return this;
	}
	
	
	/**
	 * Set the image aspect ratio anchor.
	 * 
	 * Value of 1 implies show top end of the image, 0 implies at the center, -1 implies show at the bottom.
	 * 
	 * A special value AQuery.ANCHOR_DYNAMIC will adjust the anchor base. 
	 * This setting will add up from 0 to 0.5 bias and it's suitable for portraits and common photos.
	 * 
	 * Default value is ANCHOR_DYNAMIC.
	 *
	 * @param anchor the anchor
	 * @return self
	 */
	
	public BitmapAjaxCallback anchor(float anchor){
		this.anchor = anchor;
	
		return this;
	}
	
	/**
	 * Set the image decoding options for bitmap reuse to conserve memory. 
	 * 
	 * This feature is only available on API 12+. Nothing happens otherwise.
	 *
	 * @param reuse the options for decoding image
	 * @return self
	 */
	public BitmapAjaxCallback reuse(Options reuse){
		if(AQuery.SDK_INT >= 12){		
			this.reuse = reuse;
		}
		return this;
	}
	
	
	private static Bitmap decode(String path, byte[] data, BitmapFactory.Options options){
		
		Bitmap result = null;
		
		if(path != null){
			result = BitmapFactory.decodeFile(path, options);
		}else if(data != null){
			result = BitmapFactory.decodeByteArray(data, 0, data.length, options);
		}
		
		return result;
	}
	
	private static void setField(Object object, String field, Object value){
		
		if(object == null || field == null || value == null) return;
		try{
			Field f = object.getClass().getField(field);
			f.set(object, value);
		}catch(Exception e){
			AQUtility.report(e);
		}
	}
	
	
	/**
	 * Utility method for downsampling images.
	 *
	 * @param path the file path
	 * @param data if file path is null, provide the image data directly
	 * @param target the target dimension
	 * @param width use width as target, otherwise use the higher value of height or width
	 * @param reuse the options for bitmap reuse
	 * @return the resized image
	 */
	public static Bitmap getResizedImage(String path, byte[] data, int target, boolean width, Options reuse){
    	
		Options options = null;
		
		if(reuse != null){
			
			options = reuse;
			//options.inMutable = true;
			setField(options, "inMutable", true);
			options.inSampleSize = 1;
			
		}else if(target > 0){
	    	
    		Options info = new Options();
    		info.inJustDecodeBounds = true;
    		info.inSampleSize = 1;
    		info.inScaled = false;
    		// Find the correct scale value. It should be the power of 2.
    		
	    	decode(path, data, info);
	        
	        int dim = info.outWidth;
	        if(!width) dim = Math.max(dim, info.outHeight);
	        int ssize = sampleSize(dim, target);

	        options = new Options();	        
//    		options.inJustDecodeBounds = false;
    		options.inSampleSize = ssize;
    		options.inPurgeable = true;
    		options.inDither = false;
    		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    	
    	}
        
        Bitmap bm = null;
        try{
        	bm = decode(path, data, options);
        	if(reuse != null){
        		//reuse.inBitmap = bm;
        		
        		AQUtility.debug("reused", bm.getWidth() + ":" + bm.getHeight());
        		setField(reuse, "inBitmap", bm);
        		
        	}
		}catch(OutOfMemoryError e){
			clearCache();
			AQUtility.report(e);
		}
        
        
        return bm;
    	
    }
	
    
    private static int sampleSize(int width, int target){
    	int result = 1;
		width >>= 1;    	
		while (width > target) {
			result <<= 1;
			width >>= 1;
		}
    	
//    	int result = 1;
//    	
//    	for(int i = 0; i < 10; i++){
//    		
//    		if(width < target * 2){
//    			break;
//    		}
//    		
//    		width = width / 2;
//    		result = result * 2;
//    		
//    	}
//    	
    	return result;
    }
	
    private Bitmap bmGet(String path, byte[] data){    	
    	return getResizedImage(path, data, targetWidth, targetDim, reuse);
    }
	
    @Override
    protected File accessFile(File cacheDir, String url){		
    	
    	if(imageFile != null && imageFile.exists()){
    		return imageFile;
    	}
    	
		return super.accessFile(cacheDir, url);
	}
    
    
	@Override
	protected Bitmap fileGet(String url, File file, AjaxStatus status) {		
		return bmGet(file.getAbsolutePath(), null);
	}
	
	
	
	@Override
	public Bitmap transform(String url, byte[] data, AjaxStatus status) {
		
		Bitmap bm = bmGet(null, data);
		
		if(bm == null){
			
			if(fallback > 0){			
				bm = getFallback();		
			}else if(fallback == AQuery.GONE || fallback == AQuery.INVISIBLE){
				bm = dummy;
			}
			
			if(status.getCode() != 200){
				invalid = true;
			}
		}
		
		
		return bm;
	}
	

	
	private Bitmap getFallback(){
		
		Bitmap bm = null;
		
		View view = v.get();
		if(view != null){
		
			String key = Integer.toString(fallback);			
			bm = memGet(key);
			
			if(bm == null){
				bm = BitmapFactory.decodeResource(view.getResources(), fallback);
				
				if(bm != null){
					memPut(key, bm);
				}
			}
		}
		
		return bm;
	}
	
	public static Bitmap getMemoryCached(Context context, int resId){
		
		String key = Integer.toString(resId);			
		Bitmap bm = memGet(key, 0);
		
		if(bm == null){
			bm = BitmapFactory.decodeResource(context.getResources(), resId);
			
			if(bm != null){
				memPut(key, 0, bm, false);
			}
		}
		
		return bm;
	}
	
	private static Bitmap empty = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
	public static Bitmap getEmptyBitmap(){
		return empty;
	}
	
	private static Bitmap dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);

	
	
	@Override
	public final void callback(String url, Bitmap bm, AjaxStatus status) {
		
		ImageView firstView = v.get();
		WeakHashMap<ImageView, BitmapAjaxCallback> ivs = queueMap.remove(url);
		
		//check if view queue already contains first view 
		if(ivs == null || !ivs.containsKey(firstView)){
			checkCb(this, url, firstView, bm, status);
		}
		
		if(ivs != null){
		
			Set<ImageView> set = ivs.keySet();
			
			for(ImageView view: set){
				BitmapAjaxCallback cb = ivs.get(view);
				cb.status = status;				
				checkCb(cb, url, view, bm, status);
			}
		
		}
		
	}
	
	private void checkCb(BitmapAjaxCallback cb, String url, ImageView v, Bitmap bm, AjaxStatus status){
		
		if(v == null || cb == null) return;
		
		if(url.equals(v.getTag(AQuery.TAG_URL))){			
		
			if(v instanceof ImageView){
				cb.callback(url, (ImageView) v, bm, status);
			}else{
				setBitmap(url, v, bm, false);
			}
			
		}
		
		showProgress(false);
	}
	
	protected void callback(String url, ImageView iv, Bitmap bm, AjaxStatus status){
		setBitmap(url, iv, bm, false);
	}
	

	/**
	 * Sets the icon cache size in count. Icons are images less than 50x50 pixels.
	 *
	 * @param limit the new icon cache limit
	 */
	public static void setIconCacheLimit(int limit){
		SMALL_MAX = limit;
		clearCache();
	}
	
	/**
	 * Sets the cache limit in count.
	 *
	 * @param limit the new cache limit
	 */
	public static void setCacheLimit(int limit){
		BIG_MAX = limit;
		clearCache();
	}
	
	/**
	 * Sets the pixel limit per image. Image larger than limit will not be memcached.
	 *
	 * @param pixels the new pixel limit
	 */
	public static void setPixelLimit(int pixels){
		BIG_PIXELS = pixels;
		clearCache();
	}
	
	/**
	 * Sets the pixel criteria for small images. Small images are cached in a separate cache.
	 *
	 * Default is 50x50 (2500 pixels)
	 *
	 * @param pixels the small image pixel criteria
	 */
	public static void setSmallPixel(int pixels){
		SMALL_PIXELS = pixels;
		clearCache();
	}
	
	/**
	 * Sets the max pixel limit for the entire memcache. LRU images will be expunged if max pixels limit is reached.
	 *
	 * @param pixels the new max pixel limit
	 */
	public static void setMaxPixelLimit(int pixels){
		BIG_TPIXELS = pixels;
		clearCache();
	}
	
	/**
	 * Clear the bitmap memcache.
	 */
	public static void clearCache(){
		bigCache = null;
		smallCache = null;
		invalidCache = null;
	}
	
	protected static void clearTasks(){
		queueMap.clear();
	}
	
	private static Map<String, Bitmap> getBCache(){
		if(bigCache == null){
			bigCache = Collections.synchronizedMap(new BitmapCache(BIG_MAX, BIG_PIXELS, BIG_TPIXELS));
		}
		return bigCache;
	}
	
	
	private static Map<String, Bitmap> getSCache(){
		if(smallCache == null){
			smallCache = Collections.synchronizedMap(new BitmapCache(SMALL_MAX, SMALL_PIXELS, 250000));
		}
		return smallCache;
	}
	
	private static Map<String, Bitmap> getICache(){
		if(invalidCache == null){
			invalidCache = Collections.synchronizedMap(new BitmapCache(100, BIG_PIXELS, 250000));
		}
		return invalidCache;
	}
	
	@Override
	protected Bitmap memGet(String url){		
		if(bm != null) return bm;
		if(!memCache) return null;
		return memGet(url, targetWidth);
	}
	
	
	/**
	 * Gets the memory cached bitmap.
	 *
	 * @param url the url
	 * @param targetWidth the target width, 0 for non downsampling
	 * @return the memory cached
	 */
	public static Bitmap getMemoryCached(String url, int targetWidth){
		return memGet(url, targetWidth);
	}
	
	private static Bitmap memGet(String url, int targetWidth){
		
		url = getKey(url, targetWidth);
		
		Map<String, Bitmap> cache = getBCache();
		Bitmap result = cache.get(url);
		
		if(result == null){
			cache = getSCache();
			result = cache.get(url);
		}
		
		if(result == null){
			cache = getICache();
			result = cache.get(url);
			
			if(result != null){
				
				if(getLastStatus() == 200){
					invalidCache = null;
					result = null;
				}
				
			}
		}
//		Log.d(TAG, "key url: "+url+" : "+result);

		return result;
	}
	
	private static String getKey(String url, int targetWidth){
		if(targetWidth <= 0){
			return url;
		}
		return url + "#" + targetWidth;
	}
	
	private static void memPut(String url, int targetWidth, Bitmap bm, boolean invalid){
		
		if(bm == null) return;
		
		int pixels = bm.getWidth() * bm.getHeight();
		
		Map<String, Bitmap> cache = null;
		
		if(invalid){
			cache = getICache();
		}else if(pixels <= SMALL_PIXELS){
			cache = getSCache();
		}else{
			cache = getBCache();
		}
		
		cache.put(getKey(url, targetWidth), bm);
		
	}
	
	
	@Override
	protected void memPut(String url, Bitmap bm){
		memPut(url, targetWidth, bm, invalid);
	}
	
	
	private static Bitmap filter(View iv, Bitmap bm, int fallback){
		//ignore 1x1 pixels
		if(bm != null && bm.getWidth() == 1 && bm.getHeight() == 1 && bm != empty){        
			bm = null;
		}
		
		if(bm != null){
			iv.setVisibility(View.VISIBLE);
		}else if(fallback == AQuery.GONE){
			iv.setVisibility(View.GONE);
		}else if(fallback == AQuery.INVISIBLE){
			iv.setVisibility(View.INVISIBLE);
		}
		
		return bm;
	}
	
	private void presetBitmap(String url, ImageView v){
		
		
		if(!url.equals(v.getTag(AQuery.TAG_URL)) || preset != null){			
			
			v.setTag(AQuery.TAG_URL, url);
			
			if(preset != null && !cacheAvailable(v.getContext())){
				setBitmap(url, v, preset, true);
			}else{
				
				setBitmap(url, v, null, true);
			}
		}
		
	}
	
	
	private static final int FADE_DUR = 300;
	
	private void setBitmap(String url, ImageView iv, Bitmap bm, boolean isPreset){
		
		if(bm == null){
			iv.setImageDrawable(null);
			return;
		}
		
		if(isPreset){
//			iv.setImageBitmap(bm);
			iv.setImageDrawable(makeDrawable(iv, bm, ratio, anchor, null, null));
			return;
		}
		
		if(status != null){
			setBmAnimate(iv, bm, preset, fallback, animation, ratio, anchor, status.getSource(), getCacheFile(), reuse);
		}
		
	}

	private static Drawable makeDrawable(ImageView iv, Bitmap bm, float ratio, float anchor, File file, Options reuse){
		
		BitmapDrawable bd = null;
		
		if(ratio > 0){
			bd = new RatioDrawable(iv.getResources(), bm, iv, ratio, anchor, file, reuse);
		}else{
			bd = new BitmapDrawable(iv.getResources(), bm);
		}
		return bd;
		
	}
	
	private static void setBmAnimate(ImageView iv, Bitmap bm, Bitmap preset, int fallback, int animation, float ratio, float anchor, int source, File file, Options reuse){
		
		bm = filter(iv, bm, fallback);
		if(bm == null){
			iv.setImageBitmap(null);
			return;
		}
		
		Drawable d = makeDrawable(iv, bm, ratio, anchor, file, reuse);
		Animation anim = null;
		
		if(fadeIn(animation, source)){	
			if(preset == null){
				anim = new AlphaAnimation(0, 1);
				anim.setInterpolator(new DecelerateInterpolator()); 
				anim.setDuration(FADE_DUR);
			}else{
				
				Drawable pd = makeDrawable(iv, preset, ratio, anchor, null, null);
				Drawable[] ds = new Drawable[]{pd, d};
				TransitionDrawable td = new TransitionDrawable(ds);
				td.setCrossFadeEnabled(true);				
				td.startTransition(FADE_DUR);
				d = td;
			}
		}else if(animation > 0){
			anim = AnimationUtils.loadAnimation(iv.getContext(), animation);
		}
		
		iv.setImageDrawable(d);
		
		if(anim != null){
			anim.setStartTime(AnimationUtils.currentAnimationTimeMillis());		
			iv.startAnimation(anim);
		}else{
			iv.setAnimation(null);
		}
	}
	
	private static boolean fadeIn(int animation, int source){
		
		switch(animation){
			case AQuery.FADE_IN:
				return true;
			case AQuery.FADE_IN_FILE:
				if(source == AjaxStatus.FILE) return true;		
			case AQuery.FADE_IN_NETWORK:
				if(source == AjaxStatus.NETWORK) return true;
			default:
				return false;
		}
		
	}

	/**
	 * AQuery internal use only. Please uses AQuery image() methods instead.
	 * 
	 * Optimize memory usage if mem hit and there's no custom callback.
	 *
	 *
	 */
	
	public static void async(Activity act, Context context, ImageView iv, String url, boolean memCache, boolean fileCache, int targetWidth, int fallbackId, Bitmap preset, int animation, float ratio, float anchor, View progress){
		
		Bitmap bm = null;
		
		if(memCache){
			bm = memGet(url, targetWidth);
		}
		
		if(bm != null){
			iv.setTag(AQuery.TAG_URL, url);
			if(progress != null) progress.setVisibility(View.GONE);		
			setBmAnimate(iv, bm, preset, fallbackId, animation, ratio, anchor, AjaxStatus.MEMORY, null, null);
		}else{
			BitmapAjaxCallback cb = new BitmapAjaxCallback();			
			cb.url(url).imageView(iv).memCache(memCache).fileCache(fileCache).targetWidth(targetWidth).fallback(fallbackId).preset(preset).animation(animation).ratio(ratio).anchor(anchor).progress(progress);
			if(act != null){
				cb.async(act);
			}else{
				cb.async(context);
			}
		}
		
	}
	
	
	@Override
	public void async(Context context){
		
		
		String url = getUrl();		
		
		ImageView v = this.v.get();
		
		if(url == null){
			showProgress(false);
			setBitmap(url, v, null, false);
			return;
		}
		
		Bitmap bm = memGet(url);
		if(bm != null){		
			v.setTag(AQuery.TAG_URL, url);
			status = new AjaxStatus().source(AjaxStatus.MEMORY).done();
			callback(url, bm, status);
			return;
		}
		
		
		presetBitmap(url, v);
		
		if(!queueMap.containsKey(url)){
			addQueue(url, v);	
			super.async(v.getContext());
		}else{	
			showProgress(true);			
			addQueue(url, v);
		}
		
		
	}
	

	
	private void addQueue(String url, ImageView iv){
		
		
		WeakHashMap<ImageView, BitmapAjaxCallback> ivs = queueMap.get(url);
		
		if(ivs == null){
			
			if(queueMap.containsKey(url)){
				//already a image view fetching
				ivs = new WeakHashMap<ImageView, BitmapAjaxCallback>();
				ivs.put(iv, this);
				queueMap.put(url, ivs);
			}else{
				//register a view by putting a url with no value
				queueMap.put(url, null);
			}
			
		}else{
			//add to list of image views
			ivs.put(iv, this);
			
		}
		
	}

}
