package net.atomarea.flowx_nobind.xmpp.jingle;

import net.atomarea.flowx_nobind.entities.DownloadableFile;

public abstract class JingleTransport {
	public abstract void connect(final OnTransportConnected callback);

	public abstract void receive(final DownloadableFile file,
			final OnFileTransmissionStatusChanged callback);

	public abstract void send(final DownloadableFile file,
			final OnFileTransmissionStatusChanged callback);

	public abstract void disconnect();
}
