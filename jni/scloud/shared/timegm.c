#ifndef __TIMEGM_C__
#define __TIMEGM_C__ 1
#ifdef ANDROID

#include <time.h>
#include <stdlib.h>

time_t timegm( struct tm *tm ) {                                                                                                                                                     

  time_t returnValue;
  char *timeZone;

  timeZone = getenv( "TZ" );
  setenv( "TZ", "", 1 );
  tzset();

  returnValue = mktime( tm );

  if( timeZone ) {
    setenv( "TZ", timeZone, 1 );
  } else {
    unsetenv( "TZ" );
  }
  tzset();

  return returnValue;

}

#endif/*ANDROID*/
#endif/*__TIMEGM_C__*/
