package com.fbqr.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FbQrContactlistFav extends ListActivity {
	/** Called when the activity is first created. */
	private FbQrDatabase db=null;
	private ArrayAdapter<ContactView>  adapList=null;
	private ArrayList<ContactView> contactList=null,searchList=null;
	private SOAPConnected mSoap = new SOAPConnected(FbQrContactlistFav.this);
	private EditText filterText;
	private int posTextLength=0;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//UI
		setContentView(R.layout.contactlayout);		
		filterText = (EditText) findViewById(R.id.searchfield);
		filterText.addTextChangedListener(filterTextWatcher);		
		filterText.setImeOptions(EditorInfo.IME_ACTION_DONE);
	}
	
	public void onStart(){
		super.onStart();
		db=new FbQrDatabase(this);
		reLoading();
	}
	
	public void onResume(){
		super.onResume();
		db=new FbQrDatabase(this);
		reLoading();
	}
	
	 public void onPause(){
		 super.onPause();
		 db.close();
	 }
	 
	 public void onStop(Bundle savedInstanceState) {
	       super.onStop();
	       db.close();
	 }
	
	 private TextWatcher filterTextWatcher = new TextWatcher() {
		 
			public void afterTextChanged(Editable s) {
			}
		 
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
		 
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				 String name,uid;
				 int pos;
				 int textlength=filterText.getText().length();
				 if(textlength==0){
					 reLoading();
					 return;				 
				 }
				 else{ 
					 if(posTextLength>=textlength)
						 reLoading();
				 }
				 searchList = new ArrayList<ContactView>();  
				 for(int i=0;i<adapList.getCount();i++){
					 name=adapList.getItem(i).name;
					 if(textlength<=name.length()){
						 if(name.toLowerCase().indexOf(filterText.getText().toString().toLowerCase())>=0){
							 uid = adapList.getItem(i).uid;
							 pos = db.getIdByUid(uid);
							 searchList.add(new  ContactView(name,uid,pos));
						 }
					 }
				 }
				 contactList=searchList;
				 adapList=new FbQrArrayAdapter(FbQrContactlistFav.this,contactList);
				 FbQrContactlistFav.this.setListAdapter(adapList);
				 adapList.notifyDataSetChanged();
				 posTextLength=textlength;
			}
		};
	 
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		  super.onActivityResult(requestCode, resultCode, data);
		  if (requestCode == 2) { //login
		  		if (resultCode == RESULT_OK) {
		  			String MODE = data.getStringExtra("MODE");
		  			if(isOnline()){
			  			if(MODE.matches("getPhoneBook")){			  				
					  			Toast.makeText(this, "Downloading PhoneBook", Toast.LENGTH_LONG).show();
					  			mSoap.getPhoneBook(db.getAccessToken(),new getData());			  				
			  			}
		  			}
		  		}else if(resultCode == RESULT_CANCELED){
		  			if(data==null) return;
		  			String msg=data.getStringExtra("Error");
		  			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();		  			
		  		}
		  }
		  if (requestCode == 1) { //edit
		  		if (resultCode == RESULT_OK) {
		  			String mode=data.getStringExtra("MODE");
		  			if(mode.matches("update")){
		  				if(isOnline()){
			  				String[] uids = data.getStringArrayExtra("uids");
			  				String[] pwds = data.getStringArrayExtra("pwds");
			  				if(uids.length>0)
			  					mSoap.getFriendInfoBypass(uids, db.getAccessToken(),pwds,new getData());
		  				}
		  			}
		  			else reLoading();
		  		}
		  }
	}
	
	private void reLoading(){		
		//start activity code
		Cursor cursor=db.getFavorite();
		FbQrProfile profile;
     	contactList = new ArrayList<ContactView>();  
     	while (cursor.moveToNext()) {     		  
     		profile=db.getProfile(cursor);
     		contactList.add(new  ContactView(profile.name,profile.uid,cursor.getInt(0)));
	    }     
     	db.close();
     	adapList=new FbQrArrayAdapter(this,contactList);
     	this.setListAdapter(adapList);
     	adapList.notifyDataSetChanged();
     	startManagingCursor(cursor);
	}
	
	private void showGroup(int[] ids){		
		//start activity code
		FbQrProfile profile=null;
     	contactList = new ArrayList<ContactView>();  
     	for(int i:ids) {    
     		profile=db.getProfile(i);
     		contactList.add(new  ContactView(profile.name,profile.uid,profile.position));
	    }     
     	db.close();
     	adapList=new FbQrArrayAdapter(this,contactList);
     	this.setListAdapter(adapList);
     	adapList.notifyDataSetChanged();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {		
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(this, FbQrDisplayProfile.class);
		intent.putExtra("ID", contactList.get(position).getId());
		startActivityForResult(intent,4);
		
	}
	
	private static final int editBtnId = Menu.FIRST;
	private static final int loginBtnId = Menu.FIRST+1;
	private static final int logoutBtnId = Menu.FIRST+2;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0,editBtnId ,editBtnId,"Edit");
		if(db.getAccessToken()==null)
			menu.add(0,loginBtnId ,loginBtnId,"Login");
		else menu.add(0,logoutBtnId ,logoutBtnId,"Logout");
	    return super.onCreateOptionsMenu(menu);
	  }
	
	@Override
	public boolean onPrepareOptionsMenu (Menu menu){
		menu.removeItem(logoutBtnId);
		menu.removeItem(loginBtnId);
		if(db.getAccessToken()==null)
			menu.add(0,loginBtnId ,loginBtnId,"Login");
		else menu.add(0,logoutBtnId ,logoutBtnId,"Logout");
	    return super.onPrepareOptionsMenu(menu);		
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
	    // Handle item selection
		Intent intent;
	    switch (item.getItemId()) {
	    case editBtnId:
	    	intent= new Intent(this, FbQrContactlistEdit.class);
	    	startActivityForResult(intent,1);		
	        return true;
	    case loginBtnId:
	    	intent = new Intent(this, FbQrBackground.class);
	    	intent.putExtra("MODE", "login");
	    	startActivityForResult(intent,2);
	        return true;
	    case logoutBtnId:
	    	db.setAccessToken(null);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	public static class FbQrArrayAdapter extends ArrayAdapter<ContactView> {
		private final Activity context;
		private final String PATH = "/data/data/com.fbqr.android/files/"; 
		private LayoutInflater inflater;  		
		private final List<ContactView> contactLists;
		
		public FbQrArrayAdapter(Activity context,List<ContactView> contactLists) {			
			super(context, R.layout.rowlayout,contactLists);
			inflater = LayoutInflater.from(context) ;
			this.context=context;
			this.contactLists=contactLists;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ContactView contact = contactLists.get(position);  
		    
		   convertView = inflater.inflate(R.layout.rowlayout, null); 
		   		   
		   TextView textView = (TextView) convertView.findViewById( R.id.label );  
		   ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
	       	
	       File img=new File(PATH+contact.getUid()+".PNG");
		   if(img.exists())
		         imageView.setImageBitmap(BitmapFactory.decodeFile(img.getPath()));    

		   convertView.setTag( new ViewHolder(textView,null) );        
 		        
		    // Display planet data  
  
		    textView.setText( contact.getName() );        
		        
		    return convertView;  
		  } 			
	}
	
	protected static class ViewHolder{ 
		public ImageView imageView;
	    private CheckBox checkBox ;  
	    private TextView label;  
	    public ViewHolder() {}  
	    public ViewHolder( TextView textView, CheckBox checkBox ) {  
	      this.checkBox = checkBox ;  
	      this.label = textView ;  
	    }  
	    public CheckBox getCheckBox() {  
	      return checkBox;  
	    }  
	    public void setCheckBox(CheckBox checkBox) {  
	      this.checkBox = checkBox;  
	    }  
	    public TextView getTextView() {  
	      return label;  
	    }  
	    public void setTextView(TextView textView) {  
	      this.label = textView;  
	    }     
	    
	  }  
	
	protected static class ContactView {  
	    String name = "" ;  
	    String uid = "" ;  
	    int id;
	    boolean checked = false ;  
	    public ContactView() {}  
	    public ContactView( String name,String uid,int id ) {  
	      this.name = name ;  
	      this.uid = uid ; 
	      this.id = id ; 
	    }  
	    public ContactView( String name, boolean checked ) {  
	      this.name = name ;  
	      this.checked = checked ;  
	    }  
	    public String getName() {  
	      return name;  
	    }  
	    public void setName(String name) {  
	      this.name = name;  
	    }  
	    public void setUid(String uid){
	    	this.uid = uid;
	    }
	    public String getUid(){
	    	return uid;
	    }
	    public void setId(int id){
	    	this.id = id;
	    }
	    public int getId(){
	    	return id;
	    }
	    public boolean isChecked() {  
	      return checked;  
	    }  
	    public void setChecked(boolean checked) {  
	      this.checked = checked;  
	    }  
	    public String toString() {  
	      return name ;   
	    }  
	    public void toggleChecked() {  
	      checked = !checked ;  
	    }  
	  }  
    
	   public boolean isOnline() {		   
		    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo netInfo = cm.getActiveNetworkInfo();
		    if (netInfo != null && netInfo.isConnectedOrConnecting()){
		    	if(db.getAccessToken()!=null)
		    		return true;
		    	else{
		    		Toast.makeText(this, "Please Login", Toast.LENGTH_LONG).show();
		    		return false;
		    	}
		    }
		    else {
		    	Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
		    	return false;
		    }
		}
	
	private class getData extends SoapConnectedListener{
		@Override
		public void onComplete(final ArrayList<FbQrProfile> response) {
			// TODO Auto-generated method stub
			try {
				
				FbQrContactlistFav.this.runOnUiThread(new Runnable() {
                    public void run() {
                       FbQrProfile x;
     				   for(int i=0;i<response.size();i++){
     					   	db.addData(response.get(i));
     					    x=response.get(i);
     					    saveDisplay(x.display,x.uid);
     			       }
     				   Toast.makeText(FbQrContactlistFav.this,"Download Completed", Toast.LENGTH_LONG).show();      
     				   reLoading();
                    }
                });
			 	
			} catch (Exception e) {
				Log.w("getData", e.toString());
            } 
		}	
		public void onError(String e)
		{
         	 Toast.makeText(FbQrContactlistFav.this, e.toString(), Toast.LENGTH_LONG).show();
		}
   }
   
	private void saveDisplay(String fileUrl,String uid){
		String PATH = "/data/data/com.fbqr.android/files/";  
		
		URL myFileUrl =null; 
		Bitmap bmImg,resizedbmImg = null;
		if(fileUrl==null||uid==null) return;
		try {
			myFileUrl= new URL(fileUrl);
		} catch (MalformedURLException e) {		
			e.printStackTrace();
		}
		try {
			HttpURLConnection conn= (HttpURLConnection)myFileUrl.openConnection();
			conn.setDoInput(true);
			conn.connect();
			//int length = conn.getContentLength();
			//int[] bitmapData =new int[length];
			//byte[] bitmapData2 =new byte[length];
			InputStream is = conn.getInputStream();
		
			bmImg= BitmapFactory.decodeStream(is);
			
			//Resize to 50x50
            
            int width = bmImg.getWidth();
            int height = bmImg.getWidth();
            
            if(width!=50&&height!=50){
                int newWidth = 50;
                int newHeight = 50;
               
                // calculate the scale - in this case = 0.4f
                float scaleWidth = ((float) newWidth) / width;
                float scaleHeight = ((float) newHeight) / height;
               
                Matrix matrix = new Matrix(); // createa matrix for the manipulation
                matrix.postScale(scaleWidth, scaleHeight);  // resize the bit map
                matrix.postRotate(0);  // rotate the Bitmap
                
             // recreate the new Bitmap
                resizedbmImg = Bitmap.createBitmap(bmImg, 0, 0,width, height, matrix, true);
            
            }
			
			OutputStream outStream = null;
			File dir = new File(PATH);
			dir.mkdirs();
			File file = new File(PATH,uid+".PNG");
			   
			outStream = new FileOutputStream(file);
			if(width!=50&&height!=50) resizedbmImg.compress(Bitmap.CompressFormat.PNG, 100, outStream);
			else bmImg.compress(Bitmap.CompressFormat.PNG, 100, outStream);
			
			outStream.flush();
			outStream.close();
			   
			//Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}