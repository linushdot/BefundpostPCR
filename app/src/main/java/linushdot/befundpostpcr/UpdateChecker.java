package linushdot.befundpostpcr;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateChecker {

    private static final String URL = "https://api.github.com/repos/linushdot/BefundpostPCR/releases?page=1&per_page=1";

    private final Context context;

    public interface Handler {
        void needsUpdate(String url);
    }

    public UpdateChecker(Context context) {
        this.context = context;
    }

    public void check(final Handler handler) {
        final RequestQueue queue = Volley.newRequestQueue(context);
        final JsonArrayRequest request = new JsonArrayRequest(URL, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    if(response.length() >= 1) {
                        final JSONObject first = response.getJSONObject(0);
                        final String url = first.getString("html_url");
                        final String tag = first.getString("tag_name");
                        if(!BuildConfig.VERSION_NAME.equals(tag)) {
                            handler.needsUpdate(url);
                        }
                    }
                } catch (JSONException e) {
                    // ignore
                }
            }
        }, null);
        queue.add(request);
    }
}
