package org.andbootmgr.app

import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/*
    uint32_t win_bg_color;
    uint8_t win_radius;
    uint8_t win_border_width;
    uint32_t win_border_color;
    uint32_t global_font_size;
    char* global_font_name;
    uint32_t button_unselected_color;
    uint32_t button_unselected_size;
    uint32_t button_unselected_text_color;
    uint32_t button_selected_color;
    uint32_t button_selected_size;
    uint32_t button_selected_text_color;
    uint8_t button_radius;
    bool button_grow_default;
    uint8_t button_border_unselected_width;
    uint32_t button_border_unselected_color;
    uint8_t button_border_selected_width;
    uint32_t button_border_selected_color;
 */
@Composable
fun Themes(vm: MainActivityState) {
	Button(onClick = {
		vm.activity!!.startActivity(Intent(vm.activity!!, Simulator::class.java).apply {
			putExtra("sdCardBlock", vm.deviceInfo!!.bdev)
		})
	}) {
		Text(text = stringResource(id = R.string.simulator))
	}
}