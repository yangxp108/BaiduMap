package com.example.baidumap;

/*
 客户端  192.168.2.1用的
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
 
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.ItemizedOverlay;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.MKLocationManager;
import com.baidu.mapapi.MKOLUpdateElement;
import com.baidu.mapapi.MKOfflineMap;
import com.baidu.mapapi.MKOfflineMapListener;
import com.baidu.mapapi.MKPlanNode;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;
import com.baidu.mapapi.MKSuggestionResult;
import com.baidu.mapapi.MapActivity;
import com.baidu.mapapi.MapController;
import com.baidu.mapapi.MapView;
import com.baidu.mapapi.MyLocationOverlay;
import com.baidu.mapapi.Overlay;
import com.baidu.mapapi.OverlayItem;
import com.baidu.mapapi.PoiOverlay;
import com.baidu.mapapi.RouteOverlay;
import com.baidu.mapapi.TransitOverlay;
 
public class MyClientMapActivity extends MapActivity {
	LocationManager locationmanager;
	MapController mMapController=null;
	MapView mMapView=null;
	BMapManager mBMapMan =null;
	MKOfflineMap mOffline=null;
	ServerSocket server=null;
	Socket socket_server=null;
	Socket socket_client=null;
	BufferedReader is;
	PrintWriter out;
	 
	double  latitude=0.0;
	double longitude=0.0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

		mBMapMan = new BMapManager(getApplication());
		mBMapMan.init("4AA2949E616E069C37CDC3152E8C401A05A0035D", null);
		super.initMapActivity(mBMapMan);
		 
	    mMapView = (MapView) findViewById(R.id.bmapsView);
		mMapView.setBuiltInZoomControls(true);  //设置启用内置的缩放控件
		
		 /** 离线地图初始化  **/
        mOffline = new MKOfflineMap();
        mOffline.init(mBMapMan, new MKOfflineMapListener() {
            public void onGetOfflineMapState(int type, int state) {
                switch (type) {
	                case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
	                    {
	                        MKOLUpdateElement update = mOffline.getUpdateInfo(state);
	                        //mText.setText(String.format("%s : %d%%", update.cityName, update.ratio));
	                    }
	                    break;
	                case MKOfflineMap.TYPE_NEW_OFFLINE:
	                    Log.d("OfflineDemo", String.format("add offlinemap num:%d", state));
	                    break;
	                case MKOfflineMap.TYPE_VER_UPDATE:
	                    Log.d("OfflineDemo", String.format("new offlinemap ver"));
	                    break;
                }    
            }
        });
        /** 离线地图导入离线包 **/
        int num = mOffline.scan();
        //if (num != 0)   mText.setText(String.format("已安装%d个离线包", num));
    	GetGPSSettings();
		getLocation(); 
        
        mMapController = mMapView.getController();  // 得到mMapView的控制权,可以用它控制和驱动平移和缩放
		GeoPoint point = new GeoPoint((int) (latitude * 1E6),
		        (int) (longitude * 1E6));  //用给定的经纬度构造一个GeoPoint，单位是微度 (度 * 1E6)
		mMapController.setCenter(point);  //设置地图中心点
		mMapController.setZoom(17);    //设置地图zoom级别
		mMapView.getController().animateTo(point);
	
				
		try {
			InetAddress addr = InetAddress.getByName("192.168.2.2");

	        Socket socket = new Socket(addr, 6666);

	        System.out.println("客户端发出socket=" + socket);

	        ClientGetMessage gm = new ClientGetMessage(socket);
	        ClientSendMessage sm = new ClientSendMessage(socket);

	        Thread gt = new Thread(gm);
	        Thread st = new Thread(sm);
	        
	        gt.start();
	        st.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//服务器端在监听这个端口
	}
	
	
	 private void getLocation(){
	        // 获取位置管理服务
	        String serviceName = Context.LOCATION_SERVICE;
	        locationmanager = (LocationManager) this.getSystemService(serviceName);
	        // 查找到服务信息
	        Criteria criteria = new Criteria();
	        criteria.setAccuracy(Criteria.ACCURACY_FINE); // 高精度
	        criteria.setAltitudeRequired(false);
	        criteria.setBearingRequired(false);
	        criteria.setCostAllowed(true);
	        criteria.setPowerRequirement(Criteria.POWER_LOW); // 低功耗

	        String provider = locationmanager.getBestProvider(criteria, true); // 获取GPS信息
	        Location location = locationmanager.getLastKnownLocation(provider); // 通过GPS获取位置
	        updateToNewLocation(location);
	        // 设置监听器，自动更新的最小时间为间隔N秒(1秒为1*1000，这样写主要为了方便)或最小位移变化超过N米
	        locationmanager.requestLocationUpdates(provider, (long)1 * 1000, 0f,locationListener);
	    }
	 
	 
	private void GetGPSSettings(){
		 locationmanager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        if (locationmanager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS模块正常", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "请开启GPS！", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
        startActivityForResult(intent,0); //此为设置完成后返回到获取界面
	}
	
	// 接收消息
	class ClientGetMessage implements Runnable
	{

	    private Socket socket;
	    BufferedReader in;
	    double x=0;
	    double y=0;

	    public ClientGetMessage(Socket socket)
	    {
	        this.socket = socket;

	    }

	    public void run()
	    {
	        
	            BufferedReader in;
	            String str = "";
	            while (true)
	            {
	                try
	                {
	                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	                    str = in.readLine();
	                    String [] pos=str.split(",");
	                    x=Double.valueOf(pos[0]);
	                    y=Double.valueOf(pos[1]);
	                    
	                }
	                catch (IOException e)
	                {
	                    e.printStackTrace();
	                }

	                Toast.makeText(getApplicationContext(), "192.168.2.2的地址是 x="+x+",y="+y, Toast.LENGTH_LONG).show();
	            }
	        
	    }

	}

	// 发送消息
	class ClientSendMessage implements Runnable
	{
	    
	    PrintWriter out;
	    BufferedReader is;

	    public ClientSendMessage(Socket socket) throws IOException
	    {
	        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
	        is = new BufferedReader(new InputStreamReader(System.in));
	    }

	    public void run()
	    {

	        String input = new String();
	    
	            while (true)
	            {
	                input =String.valueOf(latitude)+","+String.valueOf(longitude);

	                out.println(input);
	                out.flush();
	                Toast.makeText(getApplicationContext(), "客户端发送自己的地址 x="+latitude+",y="+longitude ,Toast.LENGTH_SHORT).show();
	                
	                try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	            }
	    }
	}
	    
 
    private final LocationListener locationListener = new LocationListener() {  //位置变化监听器 
        // 位置发生改变后调用
            public void onLocationChanged(Location location) {
            	updateToNewLocation(location);
            	//
            }
            // provider 被用户关闭后调用
            public void onProviderDisabled(String provider){
            	updateToNewLocation( null );
            }
             // provider 被用户开启后调用
            public void onProviderEnabled(String provider){ }
            // provider 状态变化时调用
            public void onStatusChanged(String provider, int status,
            Bundle extras){ }
       };
        
    private void updateToNewLocation(Location location) {
        if (location != null) {
            latitude = location.getLatitude();
            longitude= location.getLongitude(); // 这地方要更新自己的位置的???
            
            /*
            mMapController.animateTo(new GeoPoint((int)(latitude*1E6),(int)(longitude*1E6)));  //显示自己
            String xy=String.valueOf(latitude)+"_"+String.valueOf(longitude);
            out.println(xy);
            System.out.println(" 发送经纬度:"+"x="+String.valueOf(latitude)+"y="+String.valueOf(longitude));
            out.flush();  //向 192.168.2.2发
            */
        } else {
        	System.err.println("error");
        }	
    }
    
	@Override
	protected void onDestroy() {
	    if (mBMapMan != null) {
	        mBMapMan.destroy();
	        mBMapMan = null;
	    }
	    super.onDestroy();
	}
	
	@Override
	protected void onPause() {
	    if (mBMapMan != null) {
	        mBMapMan.stop();
	    }
	    super.onPause();
	}
	
	@Override
	protected void onResume() {
	    if (mBMapMan != null) {
	        mBMapMan.start();
	    }
	    super.onResume();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}