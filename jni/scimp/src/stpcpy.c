char *stpcpy( char *to, const char *from ) {
  for( ; (*to = *from); ++from, ++to );
  return(to);
}

