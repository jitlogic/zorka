#!/usr/bin/python

import MySQLdb, json
from os.path import isdir, exists
from os import rename

db = MySQLdb.connect('localhost', 'zico', 'zico', 'zico')

# Export symbol table (to be used later)
c = db.cursor()
c.execute('select SID,NAME from SYMBOLS')
symbols = { }

for r in c:
    symbols[r[0]] = r[1]


c.close()

# Export and prepare all hosts (to be finished inside collector itself)

hosts = { }

c = db.cursor()
c.execute('select HOST_NAME,HOST_ADDR,HOST_PATH,HOST_FLAGS,MAX_SIZE,HOST_PASS,HOST_ID from HOSTS')

HOST_PROPS = """
flags=%d
size=%d
pass=%s
addr=%s
"""

def null(s):
    return "" if s is None else s

for r in c:
  h = r[2]
  hosts[r[6]] = r[0]
  if isdir("data/%s" % h):
    if not exists("data/%s/host.properties"%h):
      print "Generating data/%s/host.properties" % h
      with open("data/%s/host.properties"%h, "w") as f:
        f.write(HOST_PROPS % (3,null(r[4]*10),null(r[5]),null(r[1])))
    if not exists("data/%s/symbols.dat"):
      print "Generating data/%s/symbols.json" % h
      with open("data/%s/symbols.json"%h,"w") as f:
        f.write(json.dumps(symbols))
    if isdir("data/%s/traces" % h) and not isdir("data/%s/tdat" % h):
      print "Renaming data/%s/traces to data/%s/tdat" % (h,h)
      rename("data/%s/traces"%h, "data/%s/tdat"%h)

c.close()



# Export users

c = db.cursor()
c.execute("select USER_ID,USER_NAME,REAL_NAME,FLAGS,PASSWORD from USERS")

users = { }

for r in c:
  users[r[1]] = { "username": r[1], "realname": r[2], "flags": r[3], "password": r[4], "hosts": [ ] }

c.close()

c = db.cursor()
c.execute("select USER_ID,HOST_ID from USERS_HOSTS")

for r in c:
  if r[0] in users and r[1] in hosts:
    users[r[0]]["hosts"].append(hosts[r[1]])

c.close()

if not exists("conf/users.json"):
  print "Generating user DB ..."
  with open("conf/users.json", "w") as f:
    f.write(json.dumps(users))



# Export templates

templates = { }

c = db.cursor();
c.execute("select TEMPLATE_ID,TRACE_ID,ORDER_NUM,FLAGS,COND_TEMPLATE,COND_PATTERN,TEMPLATE from TEMPLATES")

for r in c:
  templates[r[0]] = { "id":r[0], "traceId":r[1], "order":r[2], "flags":r[3], "condTemplate":r[4], "condRegex":r[5], "template":r[6] }

c.close()

if not exists("conf/templates.json"):
  print "Generating template DB ..."
  with open("conf/templates.json", "w") as f:
    f.write(json.dumps(templates))

db.close()


#print json.dumps(symbols)


