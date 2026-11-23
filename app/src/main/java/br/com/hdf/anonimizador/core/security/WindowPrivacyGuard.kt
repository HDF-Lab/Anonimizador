package br.com.hdf.anonimizador.core.security

import android.app.Activity
import android.view.WindowManager

object WindowPrivacyGuard {

    /**
     * Bloqueia captura de tela (screenshots), gravação de tela e oculta o conteúdo
     * na pré-visualização da gaveta de Apps Recentes do Android.
     */
    fun protectSensitiveWindow(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }
}
