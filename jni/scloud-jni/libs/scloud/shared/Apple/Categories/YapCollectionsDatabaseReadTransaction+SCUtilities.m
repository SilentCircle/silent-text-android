//
//  YapCollectionsDatabaseReadTransaction+SCUtilities.h
//  ST2
//
//  Created by Vinnie Moscaritolo on 6/3/13.
//  Copyright (c) 2013 Robbie Hanson. All rights reserved.
//

#import "YapCollectionsDatabaseTransaction.h"
#import "YapCollectionsDatabasePrivate.h"
#import "YapDatabaseString.h"
#import "YapDatabaseLogging.h"
#import "YapCollectionKey.h"
#import "YapNull.h"

#import "YapCollectionsDatabaseReadTransaction+SCUtilities.h"

#import <objc/runtime.h>

#if ! __has_feature(objc_arc)
#warning This file must be compiled with ARC. Use -fobjc-arc flag (or convert project to ARC).
#endif

/**
 * Define log level for this file: OFF, ERROR, WARN, INFO, VERBOSE
 * See YapDatabaseLogging.h for more information.
**/
#if DEBUG
  static const int ydbLogLevel = YDB_LOG_LEVEL_INFO;
#else
  static const int ydbLogLevel = YDB_LOG_LEVEL_WARN;
#endif

/**
 * We want to add an instance variable to YapCollectionsDatabaseConnection for this category.
 * This can be accomplished with the use of associative properties (objc_setAssociatedObject).
 * 
 * However there are two problems.
 * First, associative properties are designed for objective-c object, and we want to add a non-obj-c sqlite_stmt.
 * Second, we need to properly deallocate/finalize the compiled sqlite_stmt when the connection is released.
 * 
 * We solve both problems in one by wrapping the sqlite statement in an objective-c object
 * and providing a proper dealloc method that finalizes the statement.
**/

@interface SQLiteStatementWrapper : NSObject

- (id)initWithStatement:(sqlite3_stmt *)statement;

@property (nonatomic, assign, readonly) sqlite3_stmt *statement;

@end

@implementation SQLiteStatementWrapper

@synthesize statement;

- (id)initWithStatement:(sqlite3_stmt *)inStatement
{
	if ((self = [super init]))
	{
		statement = inStatement;
	}
	return self;
}

- (void)dealloc
{
	sqlite_finalize_null(&statement);
}

@end

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * We add the pre-compiled statement at the connection level so it persists from transaction to transaction.
 * This is the same place YapDatabase stores its pre-compiled sqlite statements.
**/

static char customStatementKey;

@interface YapCollectionsDatabaseConnection (SCUtilities)

- (sqlite3_stmt *)getDataForKeyOnlyStatement;

@end

@implementation YapCollectionsDatabaseConnection (SCUtilities)

- (sqlite3_stmt *)getDataForKeyOnlyStatement
{
	SQLiteStatementWrapper *wrapper = objc_getAssociatedObject(self, &customStatementKey);
	if (wrapper == nil)
	{
		char *stmt = "SELECT \"collection\", \"data\" FROM \"database2\" WHERE \"key\" = ? LIMIT 1;";
		int stmtLen = (int)strlen(stmt);
		
		sqlite3_stmt *statement = NULL;
		
		int status = sqlite3_prepare_v2(db, stmt, stmtLen+1, &statement, NULL);
		if (status == SQLITE_OK)
		{
			wrapper = [[SQLiteStatementWrapper alloc] initWithStatement:statement];
			objc_setAssociatedObject(self, &customStatementKey, wrapper, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
		}
		else
		{
			YDBLogError(@"Error creating '%@': %d %s", NSStringFromSelector(_cmd), status, sqlite3_errmsg(db));
		}
	}
	
	return wrapper.statement;
}

@end

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
#pragma mark -
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@implementation YapCollectionsDatabaseReadTransaction (SCUtilities)

- (id)objectForKey:(NSString *)key 
{
	if (key == nil) return nil;
	
	// Search cache for a match.
	// The "keys" in the cache are all collection/key tuples.
	
    __block YapCollectionKey *cacheKey = nil;
	
	[connection->objectCache enumerateKeysWithBlock:^(id collectionKeyObj, BOOL *stop) {
		
		__unsafe_unretained YapCollectionKey *collectionKey = (YapCollectionKey *)collectionKeyObj;
		
		if ([collectionKey.key isEqualToString:key])
		{
			cacheKey = collectionKey;
			*stop = YES;
		}
	}];
	
	if (cacheKey)
	{
		id object = [connection->objectCache objectForKey:cacheKey];
		if (object)
			return object;
	}
	
	sqlite3_stmt *statement = [connection getDataForKeyOnlyStatement];
	if (statement == NULL) return nil;
	
	// SELECT "collection", "data" FROM "database" WHERE "key" = ? LIMIT 1;
	
	YapDatabaseString _key; MakeYapDatabaseString(&_key, key);
	sqlite3_bind_text(statement, 1, _key.str, _key.length, SQLITE_STATIC);
	
	NSString *collection = nil;
	id object = nil;
	
	int status = sqlite3_step(statement);
	if (status == SQLITE_ROW)
	{
		if (connection->needsMarkSqlLevelSharedReadLock)
			[connection markSqlLevelSharedReadLockAcquired];
		
		const unsigned char *text = sqlite3_column_text(statement, 0);
		int textSize = sqlite3_column_bytes(statement, 0);
		
		const void *blob = sqlite3_column_blob(statement, 1);
		int blobSize = sqlite3_column_bytes(statement, 1);
		
		collection = [[NSString alloc] initWithBytes:text length:textSize encoding:NSUTF8StringEncoding];
		
		// Performance tuning:
		//
		// Use initWithBytesNoCopy to avoid an extra allocation and memcpy.
		// But be sure not to call sqlite3_reset until we're done with the data.
		
		NSData *objectData = [[NSData alloc] initWithBytesNoCopy:(void *)blob length:blobSize freeWhenDone:NO];
		
		object = connection->database->objectDeserializer(objectData);
	}
	else if (status == SQLITE_ERROR)
	{
		YDBLogError(@"Error executing 'getDataForKeyStatement': %d %s, key(%@)",
                    status, sqlite3_errmsg(connection->db), key);
	}
	
	sqlite3_clear_bindings(statement);
	sqlite3_reset(statement);
	FreeYapDatabaseString(&_key);
	
	// Store in cache for future lookup
	
	if (object)
	{
		cacheKey = [[YapCollectionKey alloc] initWithCollection:collection key:key];
		
		[connection->objectCache setObject:object forKey:cacheKey];
	}
	
	return object;
}

@end
