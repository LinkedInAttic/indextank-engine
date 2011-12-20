namespace java com.flaptor.indextank.rpc
namespace py flaptor.indextank.rpc

# indexable document
struct Document {
  1: map<string,string> fields
}

struct ResultSet {
  1: string status,
  2: i32 matches,
  3: list<map<string,string>> docs,
  4: optional map<string, map<string, i32>> facets = {},
  5: optional string didyoumean
  6: optional list<map<string, string>> categories = [],
  7: optional list<map<i32, double>> variables = [],
  8: optional list<double> scores,
}

struct SearcherStats {
  1: string status,
  2: double mean_qps,
  3: double max_qps,
  4: list<string> top10
}

struct WorkerMountStats {
  1: map<string, list<i32>> fs_sizes
}

struct WorkerLoadStats {
  1: double one_minute_load
  2: double five_minutes_load
  3: double fifteen_minutes_load
}

struct IndexStats {
  2: i32 used_disk,
  3: i32 used_mem
}

struct RangeFilter {
  1: i32 key,
  2: double floor,
  3: bool no_floor,
  4: double ceil,
  5: bool no_ceil
}

# Deprecated
struct IndexerStats {
  1: string status,
  2: i32 documents,
  3: i32 size,
  4: double dipm
}

enum IndexerStatus { started, recovering, ready, error }

struct CategoryFilter {
  1: string category,
  2: string value
}

struct IndexInfo {
  1: string code,
  2: string name,
  3: string account_email,
  4: i32 document_count
  
}

exception IndextankException {
  1: string message
}

exception InvalidQueryException {
  1: string message
}

exception MissingQueryVariableException {
  1: string message
}

service Searcher {
  ResultSet search(1: string query, 2: i32 start, 3: i32 length, 4: i32 scoringFunctionIndex, 6: map<i32,double> query_variables = {}, 7: list<CategoryFilter> facetsFilter = [], 8: list<RangeFilter> variableRangeFilters = [], 9: list<RangeFilter> functionRangeFilters = [], 5: map<string,string> extra_parameters) throws (1: IndextankException ite, 2: InvalidQueryException iqe, 3: MissingQueryVariableException qve ),
  i32 count(1: string query) throws (1: IndextankException ite),
  i32 size() throws (1: IndextankException ite),
  SearcherStats stats() throws (1: IndextankException ite) 
}

service Indexer {
  void            addDoc(1:string docid, 2: Document doc, 3: i32 timestamp_boost, 4: map<i32,double> boosts) throws (1: IndextankException ite),
  void            updateTimestampBoost(1:string docid, 2: i32 timestamp_boost) throws (1: IndextankException ite),
  void            updateBoost(1:string docid, 2: map<i32,double> boosts) throws (1: IndextankException ite),
  void            updateCategories(1:string docid, 2: map<string,string> categories) throws (1: IndextankException ite),
  void            delDoc(1:string docid) throws (1: IndextankException ite),
  void            promoteResult(1:string docid, 2:string query) throws (1: IndextankException ite),
  void            dump() throws (1: IndextankException ite),
  void            addScoreFunction(1:i32 functionIndex, 2:string definition) throws (1: IndextankException ite),
  void            removeScoreFunction(1:i32 functionIndex) throws (1: IndextankException ite),
  map<i32,string> listScoreFunctions() throws (1: IndextankException ite),
  IndexerStats    stats() throws (1: IndextankException ite),
  map<string,string> get_stats() throws (1: IndextankException ite),
  void            force_gc() throws (1: IndextankException ite),
  IndexerStatus   getStatus()
  void            ping()
  void            startFullRecovery()
}

service Suggestor {
    list<string> complete(1: string partial_query, 2: string field = "")
}

service Storage {
    void enqueueAddStore(1:string indexId, 2:string docId, 3:Document document, 4: i32 timestamp_boost, 5: map<i32,double> boosts) throws (1: IndextankException ite)
    void enqueueRemoveStore(1:string indexId, 2:string docId) throws (1: IndextankException ite)
    void enqueueUpdateBoosts(1:string indexId, 2:string docId, 3: map<i32,double> boosts) throws (1: IndextankException ite)
    void enqueueUpdateTimestamp(1:string indexId, 2:string docId, 3:i32 timestamp_boost) throws (1: IndextankException ite)
    void enqueueUpdateCategories(1:string indexId, 2:string docId, 3:map<string,string> categories) throws (1: IndextankException ite)
    string sendAdminCommand(1:string command, 2: map<string,string> info = {}) throws (1: IndextankException ite)
}

exception NebuException {
  1: string message
}

service Controller {
  bool  start_engine(1: string json_configuration) throws (1: NebuException ne)
  i32   kill_engine(1: string index_code, 2: i32 base_port) throws (1: NebuException ne)
  WorkerMountStats get_worker_mount_stats() throws (1: NebuException ne)
  WorkerLoadStats get_worker_load_stats() throws (1: NebuException ne)
  IndexStats get_worker_index_stats(1:string index_code, 2:i32 port) throws (1: NebuException ne)
  void  stats() throws (1: NebuException ne) 
  i32   update_worker(1: string source_host)
  void  restart_controller()
  string head(1: string file, 2: i32 lines = 10, 3: string index_code = '', 4: i32 base_port = 0) throws (1: NebuException ne)
  string tail(1: string file, 2: i32 lines = 10, 3: string index_code = '', 4: i32 base_port = 0) throws (1: NebuException ne)
  string ps_info(1: string pidfile = '', 2: string index_code = '', 3: i32 base_port = 0) throws (1: NebuException ne)
}

service WorkerManager {
  i32   add_worker(1: string instance_type = 'm1.large') throws (1: NebuException ne)
  i32   remove_worker(1: string instance_name) throws (1: NebuException ne)
  i32   update_status(1: string instance_name) throws (1: NebuException ne)
}

service DeployManager {
  void  service_deploys() throws (1: NebuException ne)
  void  delete_index(1: string index_code) throws (1: NebuException ne)
  void  redeploy_index(1: string index_code) throws (1: NebuException ne)
}

service FrontendManager {
  list<IndexInfo> 	list_indexes()
  void	save_insight(1: string index_code, 2: string insight_code, 3: string json_value)
}

struct LogRecord {
  1: optional i64 id,
  2: optional i64 timestamp_ms,
  3: optional string index_code,
  4: string docid,
  5: bool deleted,
  6: optional map<string,string> fields,
  7: optional map<i32,double> variables,
  8: optional map<string,string> categories
}

enum PageType { initial, optimized, index, live }

struct LogPageToken {
  1: optional PageType type = 'initial',
  2: optional i64 timestamp,
  5: optional i64 file_position,
  
  7: optional PageType next_type, 
  8: optional i64 next_timestamp, 
  
  3: optional i64 OBSOLETE_1,
  4: optional string OBSOLETE_2,
  6: optional bool OBSOLETE_3,
}


struct LogBatch {
  1: list<LogRecord> records,
}
struct LogPage {
  1: LogBatch batch,
  2: optional LogPageToken next_page_token
}

service LogWriter {
  void      send_batch(1: LogBatch batch),
}

struct SegmentInfo {
  1: i64    timestamp,
  2: i64    end_timestamp,
  3: i64    valid_length,
  4: i64    actual_length,
  5: i32    record_count,
  6: bool   sorted
}

struct RawLogInfo {
  1: list<SegmentInfo>  undealt_segments,
  2: i64    dealer_next_timestamp;
}

struct IndexLogInfo {
  1: list<SegmentInfo>  optimized_segments,
  2: list<SegmentInfo>  sorted_segments,
  3: list<SegmentInfo>  unsorted_segments,
  4: i64                optimized_record_count,
  5: i64                unoptimized_record_count,
  6: i32                unoptimized_segments,
  7: i64                last_optimization_timestamp,
}

struct QueueScore {
  1: i32     priority,
  2: double  score,
}

service LogReader {
  LogPage        read_page(1: string index_code, 2: LogPageToken token)
}

service LogManager {
  IndexLogInfo              get_index_log_info(1: string index_code),
  RawLogInfo                get_raw_log_info(),
  void                      enqueue_for_optimization(1: string index_code, 2: i32 priority, 3: double score),
  void                      delete_index(1: string index_code),
  map<string, QueueScore>   get_optimization_queue(),
  list<string>              list_existing_indexes(),
}
