package com.jediterm.terminal;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jediterm.terminal.emulator.Emulator;
import com.jediterm.terminal.emulator.JediEmulator;


/**
 * Runs terminal emulator. Manages threads to send response.
 *
 * @author traff
 */
public class TerminalStarter implements TerminalOutputStream {
  private static final Logger LOG = LoggerFactory.getLogger(TerminalStarter.class);

  private final Emulator myEmulator;

  private final Terminal myTerminal;
  private final TerminalDataStream myDataStream;

  private final TtyConnector myTtyConnector;

  private final ExecutorService myEmulatorExecutor = Executors.newSingleThreadExecutor();

  public TerminalStarter(final Terminal terminal, final TtyConnector ttyConnector, TerminalDataStream dataStream) {
    myTtyConnector = ttyConnector;
    //can be implemented - just recreate channel and that's it
    myDataStream = dataStream;
    myTerminal = terminal;
    myTerminal.setTerminalOutput(this);
    myEmulator = createEmulator(myDataStream, terminal);
  }

  protected JediEmulator createEmulator(TerminalDataStream dataStream, Terminal terminal) {
    return new JediEmulator(dataStream, terminal);
  }

  private void execute(Runnable runnable) {
    if (!myEmulatorExecutor.isShutdown()) {
      myEmulatorExecutor.execute(runnable);
    }
  }

  public void start() {
    try {
      while (!Thread.currentThread().isInterrupted() && myEmulator.hasNext()) {
        myEmulator.next();
      }
    }
    catch (final InterruptedIOException e) {
      LOG.info("Terminal exiting");
    }
    catch (final Exception e) {
      if (!myTtyConnector.isConnected()) {
        myTerminal.disconnected();
        return;
      }
      LOG.error("Caught exception in terminal thread", e);
    }
  }

  public byte[] getCode(final int key, final int modifiers) {
    return myTerminal.getCodeForKey(key, modifiers);
  }

  public void postResize(final Dimension dimension, final RequestOrigin origin) {
    execute(new Runnable() {
      @Override
      public void run() {
        final Dimension pixelSize;
        synchronized (myTerminal) {
          pixelSize = myTerminal.resize(dimension, origin);
        }

        myTtyConnector.resize(dimension, pixelSize);
      }
    });
  }

  @Override
  public void sendBytes(final byte[] bytes) {
    execute(() -> {
      try {
        myTtyConnector.write(bytes);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public void sendString(final String string) {
    execute(() -> {
      try {
        myTtyConnector.write(string);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public void close() {
    execute(() -> {
      try {
        myTtyConnector.close();
      }
      catch (Exception e) {
        LOG.error("Error closing terminal", e);
      }
      finally {
        myEmulatorExecutor.shutdown();
      }
    });
  }
}
