package com.lbryio.lbry.daemon;

import android.content.Context;
import android.util.Log;


import com.lbryio.lbry.core.BlobManager;
import com.lbryio.lbry.core.Utils;
import com.lbryio.lbry.daemon.auth.AuthJSONRPCServer;

import java.io.File;
import java.io.IOException;

/**
 * Daemon: service daemon.
 *
 * @author ShangWang Zhang
 * @since Ju1. 04, 2017
 */


class Checker {
	//The looping calls the daemon runs
	public final String INTERNET_CONNECTION = "internet_connection_checker";
	public final String CONNECTION_STATUS = "connection_status_checker";
}


class FileID {
	//The different ways a file can be identified
	public final String NAME = "name";
	public final String SD_HASH = "sd_hash";
	public final String FILE_NAME = "file_name";
	public final String STREAM_HASH = "stream_hash";
	public final String CLAIM_ID = "claim_id";
	public final String OUTPOINT = "outpoint";
	public final String ROWID = "rowid";
}


//TODO add login credentials in a conf file
//TODO alert if your copy of a lbry file is out of date with the name record


class CheckInternetConnection {
	private Daemon daemon;
	public  CheckInternetConnection (Daemon daemon) {
		this.daemon = daemon;
	}

	public void set () {
		this.daemon.connected_to_internet = Utils.check_connection();
	}
}


class AlwaysSend {

	int value_generator;
	char[] args;
	char[][] kwargs;

	public AlwaysSend (int value_generator, char[] args, char[][] wargs) {
		this.value_generator = value_generator;
		this.args = args;
		this.kwargs = kwargs;
	}

//	public void set() {
//		d = defer.maybeDeferred(self.value_generator, * self.args,**self.kwargs)
//		d.addCallback(
//				lambda v:(True, v))
//		return d
//	}
}

// If an instance has a lot of blobs, this call might get very expensive.
// For reflector, with 50k blobs, it definitely has an impact on the first run
// But doesn't seem to impact performance after that.

//void calculate_available_blob_size(BlobManager blob_manager) {
//	blob_hashes = yield blob_manager.get_all_verified_blobs()
//	blobs = yield defer.DeferredList([blob_manager.get_blob(b) for b in blob_hashes])
//	defer.returnValue(sum(b.length for success, b in blobs if success and b.length))
//}




public class Daemon {

	//LBRYnet daemon, a jsonrpc interface to lbry functions

	private static final String TAG = Daemon.class.getSimpleName();

	private static final String BIN_DIR_NAME = "bin";
	private static final String DAEMON_BIN_NAME = "daemon";

	public static final int INTERVAL_ONE_MINUTE = 60;
	public static final int INTERVAL_ONE_HOUR = 3600;


	static final String INITIALIZING_CODE = "initializing";
	static final String LOADING_DB_CODE = "loading_db";
	static final String LOADING_WALLET_CODE = "loading_wallet";
	static final String LOADING_FILE_MANAGER_CODE = "loading_file_manager";
	static final String LOADING_SERVER_CODE = "loading_server";
	static final String STARTED_CODE = "started";
	static final String WAITING_FOR_FIRST_RUN_CREDITS = "waiting_for_credits";
	static final String STARTUP_STAGES[][] = {
			{ INITIALIZING_CODE, "Initializing" },
			{ LOADING_DB_CODE, "Loading databases" },
			{ LOADING_WALLET_CODE, "Catching up with the blockchain"},
			{ LOADING_FILE_MANAGER_CODE, "Setting up file manager" },
			{ LOADING_SERVER_CODE, "Starting lbrynet" },
			{ STARTED_CODE, "Started lbrynet" },
			{ WAITING_FOR_FIRST_RUN_CREDITS, "Waiting for first run credits"},
	};

	//make this consistent with the stages in Downloader.py
	static final String DOWNLOAD_METADATA_CODE = "downloading_metadata";
	static final String DOWNLOAD_TIMEOUT_CODE = "timeout";
	static final String DOWNLOAD_RUNNING_CODE = "running";
	static final String DOWNLOAD_STOPPED_CODE = "stopped";
	static final String STREAM_STAGES[][] = {
			{ INITIALIZING_CODE, "Initializing" },
			{ DOWNLOAD_METADATA_CODE, "Downloading metadata" },
			{ DOWNLOAD_RUNNING_CODE, "Started %s, got %s/%s blobs, stream status: %s" },
			{ DOWNLOAD_STOPPED_CODE, "Paused stream" },
			{ DOWNLOAD_TIMEOUT_CODE, "Stream timed out" },
	};

	static final String CONNECTION_STATUS_CONNECTED = "connected";
	static final String CONNECTION_STATUS_NETWORK = "network_connection";
//	static final JSONArray CONNECTION_MESSAGES = {
//			CONNECTION_STATUS_CONNECTED: "No connection problems detected",
//			CONNECTION_STATUS_NETWORK: "Your internet connection appears to have been interrupted",
//	};

	static final short SHORT_ID_LEN = 20;


	public boolean connected_to_internet;


	Daemon() {

	}

	/**
	 * start daemon
	 */
	private static void start(Context context, Class<?> daemonClazzName, int interval) {
		String cmd = context.getDir(BIN_DIR_NAME, Context.MODE_PRIVATE)
			.getAbsolutePath() + File.separator + DAEMON_BIN_NAME;

		/* create the command string */
		StringBuilder cmdBuilder = new StringBuilder();
		cmdBuilder.append(cmd);
		cmdBuilder.append(" -p ");
		cmdBuilder.append(context.getPackageName());
		cmdBuilder.append(" -s ");
		cmdBuilder.append(daemonClazzName.getName());
		cmdBuilder.append(" -t ");
		cmdBuilder.append(interval);

		try {
			Runtime.getRuntime().exec(cmdBuilder.toString()).waitFor();
		} catch (IOException | InterruptedException e) {
			Log.e(TAG, "start daemon error: " + e.getMessage());
		}
	}

	/**
	 * Run daemon process.
	 *
	 * @param context            context
	 * @param daemonServiceClazz the name of daemon service class
	 * @param interval           the interval to check
	 */
	public static void run(final Context context, final Class<?> daemonServiceClazz,
                           final int interval) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Command.install(context, BIN_DIR_NAME, DAEMON_BIN_NAME);
				start(context, daemonServiceClazz, interval);
			}
		}).start();
	}
}
