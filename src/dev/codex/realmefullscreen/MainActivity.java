package dev.codex.realmefullscreen;

import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.widget.*;
import android.text.*;
import java.util.*;
import java.io.*;

public final class MainActivity extends Activity {
 private SharedPreferences prefs;
 private final ArrayList<ApplicationInfo> apps=new ArrayList<>(),visible=new ArrayList<>();
 private ArrayAdapter<String> adapter;
 private String search="",scopeText="Chưa đọc scope LSPosed.";
 private TextView diagnostics;
 private Set<String> scope=Collections.emptySet();
 public static boolean hookActive(){return false;}
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  prefs=Config.local(this);
  if(!prefs.contains("enabled"))prefs.edit().putBoolean("enabled",true).putBoolean("hide",true).putBoolean("cutout",true).commit();
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);int pad=(int)(16*getResources().getDisplayMetrics().density);root.setPadding(pad,pad,pad,0);
  TextView title=new TextView(this);title.setText("Ẩn thanh trạng thái realme 1.3");title.setTextSize(23);root.addView(title);
  addSwitch(root,"Bật module","enabled");addSwitch(root,"Ẩn hàng giờ / pin / sóng","hide");addSwitch(root,"Tràn vùng camera khi ngang (cả Gboard)","cutout");
  TextView help=new TextView(this);help.setText("Áp dụng cho app và màn hình chính; giữ màn hình khóa. Kéo mép trên vẫn mở thông báo. Sau khi đổi cấu hình, mở lại app; thay đổi hook framework/scope cần reboot. Lỗ camera vẫn che nội dung tại vị trí vật lý.");root.addView(help);
  diagnostics=new TextView(this);root.addView(diagnostics);updateDiagnostics();
  LinearLayout buttons=new LinearLayout(this);
  Button check=new Button(this);check.setText("Đọc scope (root)");check.setOnClickListener(v->readScope());buttons.addView(check,new LinearLayout.LayoutParams(0,-2,1));
  Button probe=new Button(this);probe.setText("Thử bố trí");probe.setOnClickListener(v->startActivity(new Intent(this,ProbeActivity.class)));buttons.addView(probe,new LinearLayout.LayoutParams(0,-2,1));root.addView(buttons);
  EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Tìm app để đặt ngoại lệ");root.addView(query);
  ListView list=new ListView(this);adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>());list.setAdapter(adapter);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
  query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int a,int b,int c){search=s.toString().toLowerCase(Locale.ROOT);refresh();}});
  list.setOnItemClickListener((p,v,pos,id)->{ApplicationInfo app=visible.get(pos);boolean excluded=prefs.getBoolean("except."+app.packageName,false);
   new AlertDialog.Builder(this).setTitle(app.loadLabel(getPackageManager())).setSingleChoiceItems(new String[]{"Áp dụng theo công tắc chung","Ngoại lệ: dùng bố trí gốc"},excluded?1:0,(d,n)->{prefs.edit().putBoolean("except."+app.packageName,n==1).commit();d.dismiss();refresh();Toast.makeText(this,"Mở lại app để cập nhật bố trí",Toast.LENGTH_LONG).show();}).setNegativeButton("Đóng",null).show();
  });
  new Thread(()->{List<ApplicationInfo> all=getPackageManager().getInstalledApplications(0);all.sort(Comparator.comparing(a->a.loadLabel(getPackageManager()).toString()));runOnUiThread(()->{apps.addAll(all);refresh();});},"Fullscreen-apps").start();
 }
 private void addSwitch(LinearLayout root,String label,String key){Switch s=new Switch(this);s.setText(label);s.setChecked(prefs.getBoolean(key,true));s.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean(key,on).commit();Toast.makeText(this,"Mở lại app để áp dụng; SystemUI có thể cần khóa/mở khóa",0).show();});root.addView(s);}
 private void updateDiagnostics(){diagnostics.setText("Hook tiến trình này: "+(hookActive()?"đã nạp":"chưa nạp")+"\n"+scopeText);}
 private void refresh(){if(adapter==null)return;adapter.clear();visible.clear();for(ApplicationInfo a:apps){String label=a.loadLabel(getPackageManager()).toString();if(!search.isEmpty()&&!label.toLowerCase(Locale.ROOT).contains(search)&&!a.packageName.contains(search))continue;visible.add(a);adapter.add(label+"\n"+a.packageName+" — "+(prefs.getBoolean("except."+a.packageName,false)?"ngoại lệ":"áp dụng")+"\n"+(scope.isEmpty()?"Scope: chưa xác minh":scope.contains(a.packageName)?"Đã chọn scope":"CHƯA chọn scope app (framework vẫn có thể áp dụng)"));}adapter.notifyDataSetChanged();}
 private void readScope(){
  diagnostics.setText("Đang đọc LSPosed bằng root…");
  new Thread(()->{try{
   String apk=getApplicationInfo().sourceDir;
   Process p=new ProcessBuilder("su","-c","CLASSPATH='"+apk.replace("'","'\\''")+"' app_process /system/bin dev.codex.realmefullscreen.ScopeTool").redirectErrorStream(true).start();
   ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=p.getInputStream().read(b))!=-1)out.write(b,0,n);int exit=p.waitFor();String text=out.toString("UTF-8");
   Set<String> found=new HashSet<>();for(String line:text.split("\\r?\\n"))if(line.startsWith("SCOPE:"))found.add(line.substring(6));
   runOnUiThread(()->{scope=found;scopeText=exit==0?text:"Không đọc được scope: "+text;updateDiagnostics();refresh();});
  }catch(Exception e){runOnUiThread(()->{scopeText="Không đọc được scope: "+e;updateDiagnostics();});}},"Fullscreen-scope").start();
 }
}
