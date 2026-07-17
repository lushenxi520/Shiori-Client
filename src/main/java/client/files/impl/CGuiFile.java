package client.files.impl;

import client.files.ClientFile;
import client.ui.ClickGUI;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class CGuiFile extends ClientFile {
   public CGuiFile() {
      super("clickgui.cfg");
   }

   @Override
   public void read(BufferedReader reader) throws IOException {
      try {
         String line;
         while ((line = reader.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts.length == 3) {
               String categoryName = parts[0];
               int x = Integer.parseInt(parts[1]);
               int y = Integer.parseInt(parts[2]);
               ClickGUI.panelPositions.put(categoryName, new int[]{x, y});
            }
         }
      } catch (Exception e) {
      }
   }

   @Override
   public void save(BufferedWriter writer) throws IOException {
      for (java.util.Map.Entry<String, int[]> entry : ClickGUI.panelPositions.entrySet()) {
         writer.write(entry.getKey() + ":" + entry.getValue()[0] + ":" + entry.getValue()[1] + "\n");
      }
   }
}