sep=','
if [ -n $1 ]; then
	sep=$1
fi
pad=15
if [ -n $2 ]; then
	pad=$2
fi
awk -v p=$pad '
{
	  for(i=1;i<=NF-1;i++)
		   printf("%-*s%s", p, $i, FS); 
	   printf("%-*s", p, $i, FS); printf("\n")
}' FS=$sep
