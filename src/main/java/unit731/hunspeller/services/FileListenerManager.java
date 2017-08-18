package unit731.hunspeller.services;

import java.io.File;
import java.util.Objects;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;


@Slf4j
public class FileListenerManager{

	private StandardFileSystemManager fsManager;
	private DefaultFileMonitor fm;
	@Getter private boolean started;

	static{
		//Disable annoying VFS log messages like:
		//	org.apache.commons.vfs2.impl.StandardFileSystemManager info
		//	INFO: Using "C:\Users\...\AppData\Local\Temp\vfs_cache" as temporary files store.
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
	}


	public FileListenerManager(FileListener fl){
		Objects.nonNull(fl);

		try{
			fsManager = (StandardFileSystemManager)VFS.getManager();

			fm = new DefaultFileMonitor(fl);

			start();
		}
		catch(FileSystemException e){
			log.error(null, e);
		}
	}

	public boolean addFile(File file){
		if(fsManager != null && fm != null){
			try{
				fm.addFile(fsManager.resolveFile(file, file.getAbsolutePath()));

				return true;
			}
			catch(FileSystemException e){
				log.error(null, e);
			}
		}
		return false;
	}

	public final void start(){
		if(!started){
			fm.start();
			started = true;
		}
	}

	public final void stop(){
		fm.stop();

		started = false;
	}

}
