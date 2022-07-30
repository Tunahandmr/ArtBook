package com.tunahan.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.tunahan.artbook.databinding.ActivityArtBinding;
import com.tunahan.artbook.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.net.URI;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> ActivityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        startlauncher();
        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.equals("new")){
            //new art
            binding.nameText.setText("");
            binding.artistText.setText("");
            binding.yearText.setText("");
            binding.button.setVisibility(View.VISIBLE);
            binding.imageView.setImageResource(R.drawable.select);


        }else {
            int artId =intent.getIntExtra("ArtId",1);
            binding.button.setVisibility(View.INVISIBLE);

            try {
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?",new String[] {String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artname");
                int artistNameIx = cursor.getColumnIndex("artistname");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){

                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.artistText.setText(cursor.getString(artistNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
                cursor.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }



    public void save(View view) {
        String name = binding.nameText.getText().toString();
        String artistName = binding.artistText.getText().toString();
        String year = binding.yearText.getText().toString();
        Bitmap smallImage = kucukGorsel(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {
            database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)");

            String sqlString = "INSERT INTO arts(artname,artistname,year,image) VALUES(? ,? ,? , ? )";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

            Intent intent = new Intent(ArtActivity.this,MainActivity.class);
            //açık bütün aktiviteleri kapa yenisini aç
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public Bitmap kucukGorsel(Bitmap image,int maximumSize){
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if(bitmapRatio>1){
            //dikey gorsel
            width = maximumSize;
            height = (int)(width/bitmapRatio);
        }else{
            //yatay gorsel
            height = maximumSize;
            width = (int)( height* bitmapRatio);
        }
        return image.createScaledBitmap(image,width,height,true);

    }

    public void selectImage(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                //bilgi ver
                Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //izin iste
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            } else {
                //izin verilmediyse iste
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }


        } else {
            //izin verildi galeriye git
            Intent intentGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            ActivityResultLauncher.launch(intentGallery);

        }

    }

    public void startlauncher() {

        ActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK) {
                    Intent intentFromResult = result.getData();
                    if (intentFromResult != null) {
                        Uri imageData = intentFromResult.getData();
                        //binding.imageView.setImageURI(imageData);
                        //bitmap

                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                //sdk 28 eşit büyükse
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            } else {
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }

                }

            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    //izin verildi galeriye git
                    Intent intentGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    ActivityResultLauncher.launch(intentGallery);
                } else {
                    //izin verilmedi
                    Toast.makeText(ArtActivity.this, "Permission needed", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
