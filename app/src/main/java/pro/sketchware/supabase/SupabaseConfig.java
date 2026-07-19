package pro.sketchware.supabase;

import com.besome.sketch.beans.ProjectLibraryBean;
import com.google.gson.Gson;
import java.io.File;
import pro.sketchware.utility.FileUtil;

public class SupabaseConfig {

    private static final String CONFIG_FILE_NAME = "supabase.json";

    public static ProjectLibraryBean load(String scId) {
        String path = getPath(scId);
        if (FileUtil.isExistFile(path)) {
            try {
                String content = FileUtil.readFile(path);
                ProjectLibraryBean bean = new Gson().fromJson(content, ProjectLibraryBean.class);
                if (bean != null) {
                    return bean;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_SUPABASE);
    }

    public static void save(String scId, ProjectLibraryBean bean) {
        String path = getPath(scId);
        try {
            String content = new Gson().toJson(bean);
            FileUtil.writeFile(path, content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getPath(String scId) {
        return FileUtil.getExternalStorageDir() + "/.sketchware/data/" + scId + "/" + CONFIG_FILE_NAME;
    }
}
