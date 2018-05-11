package org.gssb.pdffiller.pdf;

import java.io.File;
import java.util.List;

import org.gssb.pdffiller.excel.ExcelRow;

public class UnitOfWork {
   private final ExcelRow   row;
   private final List<File> generatedFiles;
   
   UnitOfWork(final ExcelRow row, final List<File> generatedFiles) {
      super();
      this.row = row;
      this.generatedFiles = generatedFiles;
   }

   public ExcelRow getRow() {
      return row;
   }

   public List<File> getGeneratedFiles() {
      return generatedFiles;
   }

}
