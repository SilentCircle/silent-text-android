//
//  YapCollectionsDatabaseReadTransaction+YapCollectionsDatabaseReadTransaction_SCUtilities.h
//  ST2
//
//  Created by Vinnie Moscaritolo on 6/3/13.
//  Copyright (c) 2013 Robbie Hanson. All rights reserved.
//

#import "YapCollectionsDatabaseTransaction.h"


@interface YapCollectionsDatabaseReadTransaction (SCUtilities)

- (id)objectForKey:(NSString *)key;

@end
