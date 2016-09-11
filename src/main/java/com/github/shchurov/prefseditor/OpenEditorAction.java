package com.github.shchurov.prefseditor;

import com.github.shchurov.prefseditor.helpers.*;
import com.github.shchurov.prefseditor.helpers.adb.AdbCommandBuilder;
import com.github.shchurov.prefseditor.helpers.adb.AdbCommandExecutor;
import com.github.shchurov.prefseditor.helpers.adb.AdbServerStarter;
import com.github.shchurov.prefseditor.helpers.exceptions.*;
import com.github.shchurov.prefseditor.model.DirectoriesBundle;
import com.github.shchurov.prefseditor.model.Preference;
import com.github.shchurov.prefseditor.ui.ChooseFileDialog;
import com.github.shchurov.prefseditor.ui.FileContentDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.List;
import java.util.Map;

public class OpenEditorAction extends AnAction {

    //TODO: handle many devices
    //TODO: handle many facets
    //TODO: add entries
    //TODO: remove entries
    //TODO: error handling

    @Override
    public void actionPerformed(AnActionEvent action) {
        try {
            Project project = ProjectUtils.getProject(action);
            AdbCommandBuilder cmdBuilder = new AdbCommandBuilder();
            AdbCommandExecutor cmdExecutor = new AdbCommandExecutor();
            new AdbServerStarter(project, cmdBuilder, cmdExecutor).start();
            DirectoriesBundle dirBundle = new DirectoriesCreator(project, cmdBuilder, cmdExecutor).createDirectories();
            Map<String, String> unifiedNamesMap = new FilesPuller(project, cmdBuilder, cmdExecutor)
                    .pullFiles(dirBundle);
            String selectedName = new ChooseFileDialog(project, unifiedNamesMap.keySet()).showAndGetFileName();
            if (selectedName == null) {
                return;
            }
            String selectedFile = dirBundle.localUnifiedDir + File.separator + unifiedNamesMap.get(selectedName);
            List<Preference> preferences = new PreferencesParser().parse(selectedFile);
            boolean save = new FileContentDialog(project, preferences).showAndGet();
            if (!save) {
                return;
            }
            new PreferencesUnparser().unparse(preferences, selectedFile);
            new FilesPusher(project, cmdBuilder, cmdExecutor).pushFiles(unifiedNamesMap, dirBundle);
        } catch (CreateDirectoriesException | ParsePreferencesException | PullFilesException | PushFilesException
                | UnparsePreferencesException | StartAdbServerException e) {
            e.printStackTrace();
        }
    }


}
