use CGI;
my $q = CGI->new;
print "content-type: text/html\n\n";
print scalar($q->param("id"));