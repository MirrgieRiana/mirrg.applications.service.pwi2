use utf8;
use strict;
use CGI;
use Encode;
use Cwd;

my $q = CGI->new();

#print $q->redirect('/a');
print $q->header(-charset => 'utf-8');

print encode("utf-8", << "END");
<meta charset="utf-8">

<h2>Form</h2>

<form method="get" action="@{[ $ENV{'REQUEST_URI'} ]}">
	GET <input type="text" name="value" value="default"><input type="submit">
</form>
<form method="post" action="@{[ $ENV{'REQUEST_URI'} ]}">
	POST <input type="text" name="value" value="default"><input type="submit">
</form>

<h2>Query</h2>

<p>
	Chdir: @{[ Cwd::getcwd() ]}<br>
	Method: @{[ $ENV{REQUEST_METHOD} ]}<br>
	Query: @{[ $ENV{QUERY_STRING} ]}<br>
	Value: @{[ decode("utf-8", $q->param("value")) ]}
</p>
<table>
	@{[ join "", map {
		<< "END2"
			<tr>
				<td><code>@{[ decode("utf-8", $_) ]}</code></td>
				<td><code>@{[ decode("utf-8", $q->param($_)) ]}</code></td>
			</tr>
END2
	} $q->param() ]}
</table>

<h2>ENV</h2>

<table>
	@{[ join "", map {
		<< "END2"
			<tr>
				<td><code>$_</code></td>
				<td><code>$ENV{$_}</code></td>
			</tr>
END2
		} sort keys %ENV ]}
</table>

END
