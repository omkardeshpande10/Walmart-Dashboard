import csv
import sqlite3
from collections import defaultdict

DB_PATH = "shipments.db"

# ---------------------------------------------------------------------------
# 1.  Set up the database
# ---------------------------------------------------------------------------
con = sqlite3.connect(DB_PATH)
cur = con.cursor()

cur.execute("""
    CREATE TABLE IF NOT EXISTS shipments (
        id                INTEGER PRIMARY KEY AUTOINCREMENT,
        origin_warehouse  TEXT    NOT NULL,
        destination_store TEXT    NOT NULL,
        product           TEXT    NOT NULL,
        on_time           BOOLEAN NOT NULL,
        product_quantity  INTEGER NOT NULL,
        driver_identifier TEXT    NOT NULL
    )
""")
con.commit()

# ---------------------------------------------------------------------------
# 2.  Helper
# ---------------------------------------------------------------------------
def str_to_bool(value: str) -> bool:
    return value.strip().lower() == "true"

def insert_row(cur, origin, destination, product, on_time, quantity, driver):
    cur.execute(
        """
        INSERT INTO shipments
            (origin_warehouse, destination_store, product,
             on_time, product_quantity, driver_identifier)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        (origin, destination, product, on_time, quantity, driver),
    )

# ---------------------------------------------------------------------------
# 3.  Spreadsheet 0 — self-contained, one row = one DB row
# ---------------------------------------------------------------------------
with open("shipping_data_0.csv", newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        insert_row(
            cur,
            row["origin_warehouse"],
            row["destination_store"],
            row["product"],
            str_to_bool(row["on_time"]),
            int(row["product_quantity"]),
            row["driver_identifier"],
        )

con.commit()
print("Spreadsheet 0: inserted.")

# ---------------------------------------------------------------------------
# 4.  Spreadsheets 1 & 2 — join on shipment_identifier
# ---------------------------------------------------------------------------

# --- Load spreadsheet 2: shipment_id -> (origin, destination, driver) -------
shipment_meta = {}
with open("shipping_data_2.csv", newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        shipment_meta[row["shipment_identifier"]] = {
            "origin_warehouse":  row["origin_warehouse"],
            "destination_store": row["destination_store"],
            "driver_identifier": row["driver_identifier"],
        }

# --- Load spreadsheet 1: group rows by (shipment_id, product, on_time) ------
# Count each (shipment_id, product) occurrence to derive product_quantity.
# on_time is consistent for every row in a shipment (same shipment = same on_time).
shipment_products = defaultdict(lambda: defaultdict(int))   # [ship_id][product] = count
shipment_on_time  = {}                                       # [ship_id] = on_time bool

with open("shipping_data_1.csv", newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        sid     = row["shipment_identifier"]
        product = row["product"]
        shipment_products[sid][product] += 1
        shipment_on_time[sid] = str_to_bool(row["on_time"])

# --- Insert one row per (shipment, product) pair ----------------------------
for sid, products in shipment_products.items():
    meta    = shipment_meta[sid]
    on_time = shipment_on_time[sid]
    for product, quantity in products.items():
        insert_row(
            cur,
            meta["origin_warehouse"],
            meta["destination_store"],
            product,
            on_time,
            quantity,
            meta["driver_identifier"],
        )

con.commit()
print("Spreadsheets 1 & 2: inserted.")

con.close()
print(f"Done. Database written to '{DB_PATH}'.")
