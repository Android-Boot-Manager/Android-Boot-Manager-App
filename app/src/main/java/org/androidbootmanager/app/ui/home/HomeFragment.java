package org.androidbootmanager.app.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;
import com.topjohnwu.superuser.io.SuFileInputStream;

import org.androidbootmanager.app.R;
import org.androidbootmanager.app.ui.installer.InstallerWelcomeWizardPageFragment;
import org.androidbootmanager.app.ui.wizard.WizardActivity;
import org.androidbootmanager.app.util.Constants;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class HomeFragment extends Fragment {

    InstalledViewModel model;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        model = new ViewModelProvider(requireActivity()).get(InstalledViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        TextView statusText0 = root.findViewById(R.id.home_installedWorking_superUser);
        TextView statusText1 = root.findViewById(R.id.home_installedWorking_install1);
        TextView statusText2 = root.findViewById(R.id.home_installedWorking_install2);
        TextView statusText3 = root.findViewById(R.id.home_installedWorking_install3);
        MaterialButton installButton = root.findViewById(R.id.home_installButton);
        AtomicBoolean check0 = new AtomicBoolean(false);
        AtomicBoolean check1 = new AtomicBoolean(false);
        AtomicBoolean check2 = new AtomicBoolean(false);
        AtomicBoolean check3 = new AtomicBoolean(false);
        ImageView statusImg = root.findViewById(R.id.home_installedWorking_image);
        Shell.su(Constants.scriptDir + "is_installed.sh").submit((result) -> {
            check0.set(Shell.rootAccess());
            statusText0.setText(check0.get() ? R.string.ok : R.string.failure);
            check1.set(result.getCode() == 0);
            statusText1.setText(check1.get() ? R.string.ok : R.string.failure);
            check2.set(String.join("",result.getOut()).contains("ABM.bootloader=1"));
            statusText2.setText(check2.get() ? R.string.ok : R.string.failure);
            check3.set(SuFile.open("/data/abm/codename.cfg").exists());
            statusText3.setText(check3.get() ? R.string.ok : R.string.failure);
            statusImg.setImageDrawable(ContextCompat.getDrawable(requireActivity(),check1.get() && check2.get() && check3.get() ? R.drawable.ic_ok : R.drawable.ic_no));
            installButton.setVisibility((!(check1.get() && check2.get()) && (!check3.get())) ? View.VISIBLE : View.INVISIBLE);
            try {
                byte[] buf = new byte[100];
                int len;
                ByteArrayOutputStream s = new ByteArrayOutputStream();
                SuFileInputStream i = new SuFileInputStream("/data/abm/codename.cfg");
                while ((len = i.read(buf)) > 0)
                    s.write(buf, 0, len);
                model.setCodename(s.toString("UTF-8").replace("\n",""));
            } catch (IOException e) {
                e.printStackTrace();
            }
            installButton.setOnClickListener((v) -> startActivity(new Intent(requireActivity(), WizardActivity.class).putExtra("StartFragment", InstallerWelcomeWizardPageFragment.class)));
            ((NavigationView) requireActivity().findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_generalcfg).setEnabled(check1.get() && check2.get() && check3.get());
            ((NavigationView) requireActivity().findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_roms).setEnabled(check1.get() && check2.get() && check3.get());
            ((NavigationView) requireActivity().findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_sd).setEnabled(check1.get() && check2.get() && check3.get());
        });
        return root;
    }
}