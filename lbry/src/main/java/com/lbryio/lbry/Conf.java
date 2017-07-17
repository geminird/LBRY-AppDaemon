package com.lbryio.lbry;

import org.json.JSONObject;

/**
 * Created by zhang on 7/12/17.
 */

public class Conf {

    public static Config settings;


    public class Config extends Object {
        public boolean  use_auth_http = false;
        public String   data_dir;
        public String   download_directory;
        public String   BLOBFILES_DIR;
        public float    data_rate;
        public JSONObject      max_key_fee;
        public int      download_timeout;
        public boolean  run_reflector_server;
        public String   wallet;
        public boolean  delete_blobs_on_remove;
        public int      peer_port;
        public int      reflector_port;
        public int      dht_node_port;
        public boolean  use_upnp;

        public String get_db_revision_filename() {
            return null;
        }

        public  String get_session_id() {
            return null;
        }
    }

}

