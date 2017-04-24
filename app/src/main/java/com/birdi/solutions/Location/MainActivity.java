package com.birdi.solutions.Location;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.gsm.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;

import static com.birdi.solutions.Location.R.*;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int SEND_SMS_PERMISSION_REQUEST_CODE = 111;

    Button datos;
    ToggleButton arranca;
    TextView latitud,longitud,resultado,distancia;
    RadioGroup aviso;

    EditText maximaDeriva,telefono;

    ObtenerWebService hiloconexion;

    public Location location, posicionFija;
    public int deriva;
    boolean activo=false; //Situación de supervision de localización, en activo analiza deriva

    LocationManager locationManager;
    LocationListener locationListener;
    AlertDialog alert = null;

    SoundPool mSoundPool;  //Generador de sonido
    int cargasonido;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        datos = (Button) findViewById(id.datos);
        latitud = (TextView) findViewById(id.latitud);
        longitud = (TextView) findViewById(id.longitud);
        distancia= (TextView) findViewById(id.distancia);
        resultado = (TextView) findViewById(id.resultado);

        aviso = (RadioGroup) findViewById(id.radio_group_aviso); //Crea el grupo de botones de aviso, y activa SMS
        aviso.check(id.radio_sms);

        mSoundPool = new SoundPool(1, AudioManager.STREAM_ALARM,0); //Creamos un sonido tipo Alarma
        cargasonido= mSoundPool.load(this, R.raw.nautical008,1);

        maximaDeriva= (EditText) findViewById(id.deriva) ;
        maximaDeriva.setText("10");
        telefono = (EditText)findViewById(id.telefono);
        arranca= (ToggleButton)findViewById(id.arranca);
        arranca.setChecked(false);




                                                  // Listener del boton arranca

        arranca.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Resources res = getResources();
                Drawable drawable;



                if (isChecked) {
                    // The toggle is enabled

                    telefono.setFocusable(false);
                    maximaDeriva.setFocusable(false);
                    telefono.setBackgroundColor(res.getColor(color.rojo));//Rojo
                    maximaDeriva.setBackgroundColor(res.getColor(color.rojo));//Rojo
                    drawable= res.getDrawable(R.drawable.anclaazuladpr);
                    arranca.setBackgroundDrawable(drawable);
                    posicionFija=location;                   // Adquirimos valores en el instante de arranque
                    activo=true;                            // Activamos el cálculo de deriva

                } else {
                    // The toggle is disabled
                    telefono.setFocusableInTouchMode(true);
                    maximaDeriva.setFocusableInTouchMode(true);
                    drawable= res.getDrawable(R.drawable.anclaazuladr);
                    arranca.setBackgroundDrawable(drawable);
                    telefono.setBackgroundColor(res.getColor(color.blanco));//Blanco
                    maximaDeriva.setBackgroundColor(res.getColor(color.blanco));//Blanco
                    activo=false;// Desactivamos cálculo de deriva y envio de SMS´

                }

            }
        });


        datos.setOnClickListener(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        /****Mejora****/

        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            AlertNoGps();
        }
       /****Permisos****/


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Han fallado los permisos Localización", Toast.LENGTH_LONG).show();
                return;
            } else {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        } else {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        MostrarLocalizacion(location);



        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {MostrarLocalizacion(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //ACTIVA EL LISTENNER y arranca GPS manager

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);

        /* Datos de los proveedores */
        //List<String> listaProveedores = locationManager.getAllProviders();
        //LocationProvider proveedor1 = locationManager.getProvider(listaProveedores.get(0));
        //int Accuracy = proveedor1.getAccuracy();
        //boolean TieneAltitud = proveedor1.supportsAltitude();
        /*----*/

        /* Establezco criterios para buscar un proveedor */
        //Criteria criteria = new Criteria();
        //criteria.setAccuracy(Criteria.ACCURACY_FINE);
        //String mejorProveedor = locationManager.getBestProvider(criteria,true);
        /*-----*/

        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

    }

    private void AlertNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("El sistema GPS esta desactivado, ¿Desea activarlo?")
                .setCancelable(false)
                .setPositiveButton("Si", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        alert = builder.create();
        alert.show();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(alert != null)
        {
            alert.dismiss ();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            } else {
                locationManager.removeUpdates(locationListener);
            }
        } else {
            locationManager.removeUpdates(locationListener);
        }


    }
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
            }
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
        }



    }

    public void MostrarLocalizacion(Location loc){
        DecimalFormat formatoSeisDecimales = new DecimalFormat("#.#######");
        DecimalFormat formatoDosDecimales = new DecimalFormat("#.##");
        float distance;

        if (loc!=null){

            //Lanza la petición de datos WebService de Google

            //hiloconexion = new ObtenerWebService();
            //hiloconexion.execute(String.valueOf(loc.getLatitude()),String.valueOf(loc.getLongitude()));

            String lat=String.valueOf(formatoSeisDecimales.format(loc.getLatitude()));
            lat=lat.replace(",",".");
            latitud.setText(lat);
            String longi=String.valueOf(formatoSeisDecimales.format(loc.getLongitude()));
            longi=longi.replace(",",".");
            longitud.setText(longi);
            location=loc; //Actualiza location con el nuevo valor
            //Calcula distancia y arranca avisos, si procede

            if (activo==true){

                    distance = location.distanceTo(posicionFija);
                    distancia.setText(String.valueOf(formatoDosDecimales.format(distance)));
                    if (distance>=Integer.parseInt(maximaDeriva.getText().toString())){
                        //resultado.setText("Superado Límite de Deriva");
                        if (aviso.getCheckedRadioButtonId()== id.radio_sms){
                            String phoneNo = telefono.getText().toString();
                            String sms = "Superada Deriva"+"\n LAT:"+lat+ "\n LON:"+longi+ "\nhttp://google.com/maps/place/" + lat + "," + longi + " ";
                            sms=sms+resultado.getText().toString();
                            enviaSms(phoneNo,sms);
                        }else if(aviso.getCheckedRadioButtonId()==id.radio_bocina){
                            mSoundPool.play(cargasonido,1,1,0,3,1);
                        }

                        //activo=false; // Suspendemos el estado activo y reestablecemos posición fija
                        posicionFija=location; //Tomamos una nueva posición Fija

                    }

                }
            }
        }

//Méto de verificación de Aviso por SMS o Alarma

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case id.radio_sms:
                if (checked)
                    // Pulsado SMS
                    break;
            case id.radio_bocina:
                if (checked)
                    // Pulsado Alarma
                    break;
        }
    }


    public void enviaSms(String Ntelefono, String TextoSMS){



        arranca.setEnabled(false);
        if(checkPermission(Manifest.permission.SEND_SMS)) {
            arranca.setEnabled(true);
        }else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.SEND_SMS}, SEND_SMS_PERMISSION_REQUEST_CODE);
        }


        if(!TextUtils.isEmpty(TextoSMS) && !TextUtils.isEmpty(Ntelefono)) {
            if(checkPermission(Manifest.permission.SEND_SMS)) {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(Ntelefono, null, TextoSMS, null, null);
            }else {
                Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                }

            }
    }


    private boolean checkPermission(String permission) {
            int checkPermission = ContextCompat.checkSelfPermission(this, permission);
            return (checkPermission == PackageManager.PERMISSION_GRANTED);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            switch (requestCode) {
                case SEND_SMS_PERMISSION_REQUEST_CODE: {
                    if(grantResults.length > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        arranca.setEnabled(true);
                    }
                    return;
                }
            }
        }


    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case id.datos:
                //hiloconexion = new ObtenerWebService();
                //hiloconexion.execute(String.valueOf(loc.getLatitude()),String.valueOf(loc.getLongitude()));
                hiloconexion = new ObtenerWebService();
                hiloconexion.execute(String.valueOf(location.getLatitude()),String.valueOf(location.getLongitude()));

                //hiloconexion = new ObtenerWebService();
                //hiloconexion.execute(latitud.getText().toString(),longitud.getText().toString());   // Parámetros que recibe doInBackground


                break;

            default:
                break;

        }

    }

    public class ObtenerWebService extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {

            String cadena = "http://maps.googleapis.com/maps/api/geocode/json?latlng=";

            //http://maps.googleapis.com/maps/api/geocode/json?latlng=38.404593,-0.529534&sensor=false
            cadena = cadena + params[0];
            cadena = cadena + ",";
            cadena = cadena + params[1];
            cadena = cadena + "&sensor=false";


            String devuelve = "";

            URL url = null; // Url de donde queremos obtener información
            try {
                url = new URL(cadena);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection(); //Abrir la conexión
                connection.setRequestProperty("User-Agent", "Mozilla/5.0" +
                        " (Linux; Android 1.5; es-ES) Ejemplo HTTP");
                //connection.setHeader("content-type", "application/json");

                int respuesta = connection.getResponseCode();
                StringBuilder result = new StringBuilder();

                if (respuesta == HttpURLConnection.HTTP_OK){


                    InputStream in = new BufferedInputStream(connection.getInputStream());  // preparo la cadena de entrada

                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));  // la introduzco en un BufferedReader

                    // El siguiente proceso lo hago porque el JSONOBject necesita un String y tengo
                    // que tranformar el BufferedReader a String. Esto lo hago a traves de un
                    // StringBuilder.

                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);        // Paso toda la entrada al StringBuilder
                    }

                    //Creamos un objeto JSONObject para poder acceder a los atributos (campos) del objeto.
                    JSONObject respuestaJSON = new JSONObject(result.toString());   //Creo un JSONObject a partir del StringBuilder pasado a cadena
                    //Accedemos al vector de resultados
                    JSONArray resultJSON = respuestaJSON.getJSONArray("results");   // results es el nombre del campo en el JSON

                    //Vamos obteniendo todos los campos que nos interesen.
                    //En este caso obtenemos la primera dirección de los resultados.
                    String direccion="SIN DATOS PARA ESA LONGITUD Y LATITUD";
                    if (resultJSON.length()>0){
                        direccion = resultJSON.getJSONObject(0).getString("formatted_address");    // dentro del results pasamos a Objeto la seccion formated_address
                    }
                    devuelve = "Location: " + direccion;   // variable de salida que mandaré al onPostExecute para que actualice la UI

                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
           return devuelve;
        }

        @Override
        protected void onCancelled(String aVoid) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                super.onCancelled(aVoid);
            }
        }

        @Override
        protected void onPostExecute(String aVoid) {
            resultado.setText(aVoid);
            Log.i("GPS",aVoid);

            //super.onPostExecute(aVoid);
        }

        @Override
        protected void onPreExecute() {
            resultado.setText("");
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }


}
