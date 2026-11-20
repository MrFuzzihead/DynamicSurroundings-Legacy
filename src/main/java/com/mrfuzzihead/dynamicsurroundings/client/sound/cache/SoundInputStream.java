package org.blockartistry.mod.DynSurround.client.sound.cache;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class SoundInputStream extends InputStream {
   protected final InputStream stream;

   public SoundInputStream(@Nonnull InputStream stream) {
      this.stream = stream;
   }

   @Override
   public int read() throws IOException {
      try {
         return this.stream.read();
      } catch (Throwable var2) {
         return -1;
      }
   }

   @Override
   public int available() throws IOException {
      try {
         return this.stream.available();
      } catch (Throwable var2) {
         return 0;
      }
   }

   @Override
   public void close() {
      try {
         this.stream.close();
      } catch (Throwable var2) {
      }
   }

   @Override
   public void mark(int readlimit) {
      this.stream.mark(readlimit);
   }

   @Override
   public boolean markSupported() {
      return this.stream.markSupported();
   }

   @Override
   public void reset() throws IOException {
      this.stream.reset();
   }
}
