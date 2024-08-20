package org.andbootmgr.app

import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toFile
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.topjohnwu.superuser.io.SuFileOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest

class WizardPageFactory(private val vm: WizardActivityState) {
	fun get(flow: String): List<IWizardPage> {
		return when (flow) {
			"droidboot" -> DroidBootWizardPageFactory(vm).get()
			"fix_droidboot" -> FixDroidBootWizardPageFactory(vm).get()
			"update_droidboot" -> UpdateDroidBootWizardPageFactory(vm).get()
			"create_part" -> CreatePartWizardPageFactory(vm).get()
			"backup_restore" -> BackupRestoreWizardPageFactory(vm).get()
			"update" -> UpdateFlowWizardPageFactory(vm).get()
			else -> listOf()
		}
	}
}

@Composable
fun WizardCompat(mvm: MainActivityState, flow: String) {
	DisposableEffect(Unit) {
		mvm.activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		onDispose { mvm.activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
	}
	val vm = remember { WizardActivityState(mvm) }
	vm.navController = rememberNavController()
	val wizardPages = remember(flow) { WizardPageFactory(vm).get(flow) }
		NavHost(
			navController = vm.navController,
			startDestination = "start",
			modifier = Modifier
				.fillMaxWidth()
		) {
			for (i in wizardPages) {
				composable(i.name) {
					Column(modifier = Modifier.fillMaxSize()) {
						BackHandler {
							(vm.onPrev ?: i.prev.onClick)(vm)
						}
						Box(Modifier.fillMaxWidth().weight(1f)) {
							i.run()
						}
						Box(Modifier.fillMaxWidth()) {
							BasicButtonRow(vm.prevText ?: i.prev.text,
								{ (vm.onPrev ?: i.prev.onClick)(vm) },
								vm.nextText ?: i.next.text,
								{ (vm.onNext ?: i.next.onClick)(vm) })
						}
					}
				}
			}
		}
}

private class ExpectedDigestInputStream(stream: InputStream?,
                                        digest: MessageDigest?,
                                        private val expectedDigest: String
) : DigestInputStream(stream, digest) {
	@OptIn(ExperimentalStdlibApi::class)
	fun doAssert() {
		val hash = digest.digest().toHexString()
		if (hash != expectedDigest)
			throw HashMismatchException("digest $hash does not match expected hash $expectedDigest")
	}
}
class HashMismatchException(message: String) : Exception(message)

class WizardActivityState(val mvm: MainActivityState) {
	val codename = mvm.deviceInfo!!.codename
	val activity = mvm.activity!!
	lateinit var navController: NavHostController
	val logic = mvm.logic!!
	val deviceInfo = mvm.deviceInfo!!
	var prevText by mutableStateOf<String?>(null)
	var nextText by mutableStateOf<String?>(null)
	var onPrev by mutableStateOf<((WizardActivityState) -> Unit)?>(null)
	var onNext by mutableStateOf<((WizardActivityState) -> Unit)?>(null)

	var flashes: HashMap<String, Pair<Uri, String?>> = HashMap()
	var texts: HashMap<String, String> = HashMap()

	fun navigate(next: String) {
		prevText = null
		nextText = null
		onPrev = null
		onNext = null
		navController.navigate(next) {
			launchSingleTop = true
		}
	}
	fun finish() {
		mvm.init()
		mvm.wizardCompat = null
		mvm.wizardCompatE = null
		mvm.wizardCompatPid = null
		mvm.wizardCompatSid = null
	}

	fun copy(inputStream: InputStream, outputStream: OutputStream): Long {
		var nread = 0L
		val buf = ByteArray(8192)
		var n: Int
		while (inputStream.read(buf).also { n = it } > 0) {
			outputStream.write(buf, 0, n)
			nread += n.toLong()
		}
		inputStream.close()
		outputStream.flush()
		outputStream.close()
		if (inputStream is ExpectedDigestInputStream)
			inputStream.doAssert()
		return nread
	}

	fun flashStream(flashType: String): InputStream {
		return flashes[flashType]?.let {
			val i = when (it.first.scheme) {
				"content" ->
					activity.contentResolver.openInputStream(it.first)
						?: throw IOException("in == null")
				"file" ->
					FileInputStream(it.first.toFile())
				"http", "https" ->
					URL(it.first.toString()).openStream()
				else -> null
			}
			if (it.second != null)
				ExpectedDigestInputStream(i, MessageDigest.getInstance("SHA-256"), it.second!!)
			else i
		} ?: throw IllegalArgumentException()
	}

	fun copyUnpriv(inputStream: InputStream, output: File) {
		Files.copy(inputStream, output.toPath(), StandardCopyOption.REPLACE_EXISTING)
		inputStream.close()
	}

	fun copyPriv(inputStream: InputStream, output: File) {
		val outStream = SuFileOutputStream.open(output)
		copy(inputStream, outStream)
	}
}

class NavButton(val text: String, val onClick: (WizardActivityState) -> Unit)
class WizardPage(override val name: String, override val prev: NavButton,
                       override val next: NavButton, override val run: @Composable () -> Unit
) : IWizardPage

interface IWizardPage {
	val name: String
	val prev: NavButton
	val next: NavButton
	val run: @Composable () -> Unit
}

@Composable
fun BasicButtonRow(prev: String, onPrev: () -> Unit,
                   next: String, onNext: () -> Unit) {
	Row {
		TextButton(onClick = {
			onPrev()
		}, modifier = Modifier.weight(1f, true)) {
			Text(prev)
		}
		TextButton(onClick = {
			onNext()
		}, modifier = Modifier.weight(1f, true)) {
			Text(next)
		}
	}
}