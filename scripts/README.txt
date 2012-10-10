Gizmo utility scripts.

Note:
All scripts are searched first in the gizmo directory and then in the gizmo/scripts directory.
Some scripts were intended to be edited for configuration.  To do so, simply copy a script
from gizmo/scripts to gizmo and edit it.  Your edited script will take priority over the
distributed script and wont get wiped away if you update your tree from svn.

FILES:
    BUGS.txt        - notes about bugs in scripts

Generic Scripts:
    b64.py          - base64 encode $BUF
    de64.py         - base64 decode $BUF
    browser.py      - open a page in a browser.  
    formedit.py     - create a form for the parameters in $BUF and open in a browser
    paramedit.py    - open an editor to edit the parameters in $BUF

Windows Specific:
    frm.bat         - replace $BUF with output of cmd
    to.bat          - send $BUF as input to cmd
    filt.bat        - send $BUF as input to cmd and replace $BUF with output
    cyg.bat         - run cygwin command
    vi.bat          - run cygwin vim on $BUF
    bvi.bat         - run cygwin bvi on $BUF to hex edit it

Unix Specific:
    frm             - replace $BUF with output of cmd
    to              - send $BUF as input to cmd
    filt            - send $BUF as input to cmd and replace $BUF with output
    winvi           - open a new window with vim editing $BUF
    winbvi          - open a new window with bvi editing $BUF to hex edit it

