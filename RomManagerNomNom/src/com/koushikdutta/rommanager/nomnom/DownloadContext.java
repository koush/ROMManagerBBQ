package com.koushikdutta.rommanager.nomnom;


public interface DownloadContext  {
	public boolean getDestroyed();
	public void onDownloadComplete(String filename);
	public void onDownloadFailed();
}
