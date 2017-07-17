package com.lbryio.lbry.daemon;

import android.app.IntentService;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;


import com.lbryio.lbry.Conf;
import com.lbryio.lbry.Manager;
import com.lbryio.lbry.core.BlobManager;
import com.lbryio.lbry.core.StreamDescriptorIdentifier;
import com.lbryio.lbry.core.Utils;
import com.lbryio.lbry.daemon.auth.AuthJSONRPCServer;
import com.lbryio.lbry.file_manager.EncryptedFileManager;
import com.lbryio.lbry.lbry_file.DBEncryptedFileMetadataManager;

import org.json.JSONException;
import org.json.JSONObject;

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
	public static String INTERNET_CONNECTION = "internet_connection_checker";
	public static String CONNECTION_STATUS = "connection_status_checker";
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


class CheckInternetConnection implements Runnable {
	private Daemon daemon;
	private final Handler handler = new Handler();
	private final int delayTime = 3600;

	public  CheckInternetConnection(Daemon daemon) {
		this.daemon = daemon;
	}

	@Override
	public void run() {
		this.checkConnection();
		handler.postDelayed(this, delayTime);
	}

	public void start() {
		handler.postDelayed(this, delayTime);
	}

	public void stop() {
		handler.removeCallbacks(this);
	}

	public void checkConnection () {

		this.daemon.connected_to_internet = Utils.check_connection();
	}
}


class UpdateConnection implements Runnable {
	private Daemon daemon;
	private final Handler handler = new Handler();
	private final int delayTime = 30;

	public  UpdateConnection(Daemon daemon) {
		this.daemon = daemon;
	}

	@Override
	public void run() {
		this._update_connection_status();
		handler.postDelayed(this, delayTime);
	}

	public void start() {
		handler.postDelayed(this, delayTime);
	}

	public void stop() {
		handler.removeCallbacks(this);
	}

	public void _update_connection_status() {
		this.daemon.connection_status_code = Daemon.CONNECTION_STATUS_CONNECTED;

		if (!this.daemon.connected_to_internet) {
			this.daemon.connection_status_code = Daemon.CONNECTION_STATUS_NETWORK;
		}
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




public class Daemon extends AuthJSONRPCServer {

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
	static final JSONObject CONNECTION_MESSAGES = new JSONObject();

	static final short SHORT_ID_LEN = 20;


	// intialize
	private String 		allowed_during_startup[] = { "stop", "status", "version"};
	private String	 	db_dir;
	private String	 	download_directory;
	private String 		blobfile_dir;
	private float 		data_rate;
	private JSONObject	max_key_fee;
	private int			download_timeout;
	private boolean		run_reflector_server;
	private String		wallet_type;
	private boolean		delete_blobs_on_remove;
	private int			peer_port;
	private int			reflector_port;
	private int			dht_node_port;
	private boolean		use_upnp;
	private String[]	startup_status;
	public 	boolean		connected_to_internet;
	public String 		connection_status_code;
	private String		platform;
	private int			current_db_revision;
	private String		db_revision_file;
	private Object		session;
	private String[]	uploaded_temp_files;
	private String		_session_id;
	private Manager		analytics_manager;
	private String		lbryid;
	private String 		wallet_user;
	private String 		wallet_password;

	private String[] 	query_handlers;
	private String[]	waiting_on;
	private	String[]	streams;
	private String[]	name_cache;

	private ExchangeRateManager		exchange_rate_manager;
	private DBEncryptedFileMetadataManager		stream_info_manager;
	private EncryptedFileManager lbry_file_manager;
	private StreamDescriptorIdentifier sd_identifier;


	Daemon(LBRYindex root, Manager analytics_manager) {
		super(Conf.settings.use_auth_http);

		try {
			CONNECTION_MESSAGES.put(CONNECTION_STATUS_CONNECTED, "No connection problems detected");
			CONNECTION_MESSAGES.put(CONNECTION_STATUS_NETWORK, "Your internet connection appears to have been interrupted");
		} catch (JSONException e) {
			e.printStackTrace();;
		}
		this.db_dir = Conf.settings.data_dir;
		this.download_directory = Conf.settings.download_directory;

		if (Conf.settings.BLOBFILES_DIR == "blobfiles") {
			this.blobfile_dir = Environment.getRootDirectory() +"/" + this.db_dir + "/"  + "blobfiles";
		} else {
			Log.i("Daemon", "Using non-default blobfiles directory: " + Conf.settings.BLOBFILES_DIR);
			this.blobfile_dir = Conf.settings.BLOBFILES_DIR;
		}

		this.data_rate = Conf.settings.data_rate;
		this.max_key_fee = Conf.settings.max_key_fee;
		this.download_timeout = Conf.settings.download_timeout;
		this.run_reflector_server = Conf.settings.run_reflector_server;
		this.wallet_type = Conf.settings.wallet;
		this.delete_blobs_on_remove = Conf.settings.delete_blobs_on_remove;
		this.peer_port = Conf.settings.peer_port;
		this.reflector_port = Conf.settings.reflector_port;
		this.dht_node_port = Conf.settings.dht_node_port;
		this.use_upnp = Conf.settings.use_upnp;

		this.startup_status = STARTUP_STAGES[0];
		this.connected_to_internet = true;
		this.connection_status_code = null;
		this.platform = null;
		this.current_db_revision = 3;
		this.db_revision_file = Conf.settings.get_db_revision_filename();
		this.session = null;
		this.uploaded_temp_files = new String[0];
		this._session_id = Conf.settings.get_session_id();
        // TODO: this should probably be passed into the daemon, or
        // possibly have the entire log upload functionality taken out
        // of the daemon, but I don't want to deal with that now

		this.analytics_manager = analytics_manager;
		this.lbryid = Utils.generate_id();

		this.wallet_user = null;
		this.wallet_password = null;
		this.query_handlers = new String[0];

		this.waiting_on = new String[0];
		this.streams = new String[0];
		this.name_cache = new String[0];
		this.exchange_rate_manager = new ExchangeRateManager();

		//batch timers
//		calls = {
//				Checker.INTERNET_CONNECTION: LoopingCall(CheckInternetConnection(self)),
//				Checker.CONNECTION_STATUS: LoopingCall(self._update_connection_status),
//        }
//		self.looping_call_manager = LoopingCallManager(calls)


		this.sd_identifier = new StreamDescriptorIdentifier();
		this.stream_info_manager = null;
		this.lbry_file_manager = null;

	}

	@Override
	public  void onDestroy() {
		this._shutdown();
	}

	public void setup() {
//		this._modify_loggly_formatter();
		Log.i("Daemon", "Starting lbrynet-daemon");

		new CheckInternetConnection(this).start();
		new UpdateConnection(this).start();
		this.exchange_rate_manager.start();
	}

	private void _shutdown() {

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
