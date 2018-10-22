package org.gssb.pdffiller.pdf;

import java.io.File;
import java.util.List;

import org.gssb.pdffiller.excel.RowGroup;

public class UnitOfWork {
   private final RowGroup   rowGroup;
   private final List<File> generatedFiles;
   
   UnitOfWork(final RowGroup rowGroup, final List<File> generatedFiles) {
      super();
      this.rowGroup = rowGroup;
      this.generatedFiles = generatedFiles;
   }

   public RowGroup getRow() {
      return rowGroup;
   }

   public List<File> getGeneratedFiles() {
      return generatedFiles;
   }

}
