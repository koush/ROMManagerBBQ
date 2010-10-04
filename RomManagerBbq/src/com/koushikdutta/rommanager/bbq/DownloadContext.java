package com.koushikdutta.rommanager.bbq;


public interface DownloadContext  {
	public boolean getDestroyed();
	public void onDownloadComplete(String filename);
	public void onDownloadFailed();
}
