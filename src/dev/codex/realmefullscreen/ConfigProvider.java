package dev.codex.realmefullscreen;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import java.util.Map;
/** Public read-only configuration. No exported operation can write preferences. */
public final class ConfigProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 @Override public Bundle call(String method,String arg,Bundle extras){
  if(!"read".equals(method))throw new SecurityException("read-only provider");
  Bundle out=new Bundle();for(Map.Entry<String,?> e:Config.local(getContext()).getAll().entrySet())if(e.getValue() instanceof Boolean)out.putBoolean(e.getKey(),(Boolean)e.getValue());return out;
 }
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){throw new UnsupportedOperationException("use read");}
 public String getType(Uri u){return "vnd.android.cursor.item/vnd.realmefullscreen.config";}
 public Uri insert(Uri u,ContentValues v){throw new SecurityException("read-only");}
 public int delete(Uri u,String s,String[] a){throw new SecurityException("read-only");}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new SecurityException("read-only");}
}
