package arcs.impl;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Context;
import android.util.Log;
import arcs.api.ClipboardSurface;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;

public class AndroidClipboardService implements ClipboardSurface {

  private Context ctx;
  private OnPrimaryClipChangedListener onPrimaryClipChangedListener;

  @Inject
  public AndroidClipboardService(@Named("AppContext") Context ctx) {
    this.ctx = ctx;
  }

  @Override
  public void listen(Consumer<String> pasted) {
    ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
    if (onPrimaryClipChangedListener != null) {
      clipboard.removePrimaryClipChangedListener(onPrimaryClipChangedListener);
    }

    onPrimaryClipChangedListener =
        () -> {
          try {
            if (clipboard.hasPrimaryClip()
                && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)
                && clipboard.getPrimaryClip().getItemCount() > 0) {
              pasted.accept(clipboard.getPrimaryClip().getItemAt(0).getText().toString());
            }
          } catch (Throwable t) {
            Log.e("Arcs", "Error in clipboard handling", t);
          }
        };
    clipboard.addPrimaryClipChangedListener(onPrimaryClipChangedListener);
  }
}
