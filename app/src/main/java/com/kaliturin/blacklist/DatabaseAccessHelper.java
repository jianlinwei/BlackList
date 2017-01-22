package com.kaliturin.blacklist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.sqlite.util.StringUtils;

import java.util.LinkedList;
import java.util.List;


/**
 * Database access helper
 */
public class DatabaseAccessHelper extends SQLiteOpenHelper {
    // TODO move to app dir
    private static final String DATABASE_NAME = "/sdcard/Download/BlackList/blacklist.db";
    //private static final String DATABASE_NAME = "/sdcard/Download/blacklist.db";
    //private static final String DATABASE_NAME = "blacklist.db";
    private static final int DATABASE_VERSION = 1;
    private static DatabaseAccessHelper sInstance = null;

    public static synchronized DatabaseAccessHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseAccessHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private DatabaseAccessHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        // helper won't create the database file until we first open it
        SQLiteDatabase db = getWritableDatabase();
        // onConfigure isn't calling in android 2.3
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(JournalTable.Statement.CREATE);
        db.execSQL(ContactTable.Statement.CREATE);
        db.execSQL(ContactNumberTable.Statement.CREATE);
        db.execSQL(SettingsTable.Statement.CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        if (i != i1) {
            db.execSQL("DROP TABLE IF EXISTS " + SettingsTable.NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ContactNumberTable.NAME);
            db.execSQL("DROP TABLE IF EXISTS " + ContactTable.NAME);
            db.execSQL("DROP TABLE IF EXISTS " + JournalTable.NAME);
            onCreate(db);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.execSQL("PRAGMA foreign_keys=ON");
    }

//----------------------------------------------------------------

    // Closes cursor if it is empty and returns false
    private boolean validate(Cursor cursor) {
        if(cursor == null || cursor.isClosed()) return false;
        if(cursor.getCount() == 0) {
            cursor.close();
            return false;
        }
        return true;
    }

    // Common statements
    private static class Common {
        /** Creates 'IN part' of 'WHERE' clause.
         *  If "all" is true - includes all items, except of specified in list.
         *  Else includes all items specified in list.
         */
        static @Nullable String getInClause(String column, boolean all, List<String> items) {
            if(all)  {
                if(items.isEmpty()) {
                    // include all items
                    return null;
                } else {
                    // include all items except of specified
                    String args = StringUtils.join(items, ", ");
                    return column + " NOT IN ( " + args + " ) ";
                }
            }
            // include all specified items
            String args = StringUtils.join(items, ", ");
            return column + " IN ( " + args + " ) ";
        }

        /** Creates 'LIKE part' of 'WHERE' clause */
        static @Nullable String getLikeClause(String column, String filter) {
            return (filter == null ? null :
                    column + " LIKE '%" + filter + "%' ");
        }

        /** Concatenates passed clauses with 'AND' operator */
        static String concatClauses(String[] clauses) {
            StringBuilder sb = new StringBuilder();
            for(String clause : clauses) {
                if(TextUtils.isEmpty(clause)) continue;
                if(sb.length() > 0) sb.append(" AND ");
                sb.append(clause);
            }
            return sb.toString();
        }
    }

    // Journal table scheme
    private static class JournalTable {
        static final String NAME = "journal";

        static class Column {
            static final String ID = "_id";
            static final String TIME = "time";
            static final String CALLER = "caller";
            static final String NUMBER = "number";
            static final String TEXT = "text";
        }

        static class Statement {
            static final String CREATE =
                    "CREATE TABLE " + JournalTable.NAME +
                            "(" +
                            Column.ID + " INTEGER PRIMARY KEY NOT NULL, " +
                            Column.TIME + " INTEGER NOT NULL, " +
                            Column.CALLER + " TEXT NOT NULL, " +
                            Column.NUMBER + " TEXT, " +
                            Column.TEXT + " TEXT " +
                            ")";

            static final String SELECT =
                    "SELECT * " +
                            " FROM " + JournalTable.NAME +
                            " ORDER BY " + Column.TIME +
                            " DESC";

            static final String SELECT_FILTER_BY_CALLER =
                    "SELECT * " +
                            " FROM " + JournalTable.NAME +
                            " WHERE " + Column.CALLER + " LIKE ? " +
                            " ORDER BY " + Column.TIME +
                            " DESC";
        }
    }

    // Journal table record
    public static class JournalRecord {
        public final long id;
        public final long time;
        public final String caller;
        public final String number;
        public final String text;

        public JournalRecord(long id, long time, @NonNull String caller,
                             String number, String text) {
            this.id = id;
            this.time = time;
            this.caller = caller;
            this.number = number;
            this.text = text;
        }
    }

    // Journal record cursor wrapper
    public class JournalRecordCursorWrapper extends CursorWrapper {
        private final int ID;
        private final int TIME;
        private final int CALLER;
        private final int NUMBER;
        private final int TEXT;

        public JournalRecordCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(JournalTable.Column.ID);
            TIME = cursor.getColumnIndex(JournalTable.Column.TIME);
            CALLER = cursor.getColumnIndex(JournalTable.Column.CALLER);
            NUMBER = cursor.getColumnIndex(JournalTable.Column.NUMBER);
            TEXT = cursor.getColumnIndex(JournalTable.Column.TEXT);
        }

        public JournalRecord getJournalRecord() {
            long id = getLong(ID);
            long time = getLong(TIME);
            String caller = getString(CALLER);
            String number = getString(NUMBER);
            String text = getString(TEXT);
            return new JournalRecord(id, time, caller, number, text);
        }
    }

    // Selects all journal records
    public @Nullable
    JournalRecordCursorWrapper getJournalRecords() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(JournalTable.Statement.SELECT, null);

        return (validate(cursor) ? new JournalRecordCursorWrapper(cursor) : null);
    }

    // Selects journal records filtered with passed filter
    public @Nullable
    JournalRecordCursorWrapper getJournalRecords(@Nullable String filter) {
        if(filter == null) {
            return getJournalRecords();
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(JournalTable.Statement.SELECT_FILTER_BY_CALLER,
                new String[] {"%" + filter + "%"});

        return (validate(cursor) ? new JournalRecordCursorWrapper(cursor) : null);
    }

    // Deletes all records specified in container and fit to filter
    public int deleteJournalRecords(IdentifiersContainer contactIds, @Nullable String filter) {
        if(contactIds.isEmpty()) return 0;

        boolean all = contactIds.isFull();
        List<String> ids = contactIds.getIdentifiers(new LinkedList<String>());

        // build 'WHERE' clause
        String clause = Common.concatClauses(new String[] {
            Common.getLikeClause(JournalTable.Column.CALLER, filter),
            Common.getInClause(JournalTable.Column.ID, all, ids)
        });

        // delete records
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(JournalTable.NAME, clause, null);
    }

    // Writes journal record
    public long addJournalRecord(long time, @NonNull String caller,
                                 String number, String text) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(JournalTable.Column.TIME, time);
        values.put(JournalTable.Column.CALLER, caller);
        values.put(JournalTable.Column.NUMBER, number);
        values.put(JournalTable.Column.TEXT, text);
        return db.insert(JournalTable.NAME, null, values);
    }

//----------------------------------------------------------------

    // Contact number table scheme
    private static class ContactNumberTable {
        static final String NAME = "number";

        static class Column {
            static final String ID = "_id";
            static final String NUMBER = "number";
            static final String TYPE = "type";
            static final String CONTACT_ID = "contact_id";
        }

        static class Statement {
            static final String CREATE =
                    "CREATE TABLE " + ContactNumberTable.NAME +
                            "(" +
                            Column.ID + " INTEGER PRIMARY KEY NOT NULL, " +
                            Column.NUMBER + " TEXT NOT NULL, " +
                            Column.TYPE + " INTEGER NOT NULL, " +
                            Column.CONTACT_ID + " INTEGER NOT NULL, " +
                            "FOREIGN KEY(" + Column.CONTACT_ID + ") REFERENCES " +
                            ContactTable.NAME + "(" + ContactTable.Column.ID + ")" +
                            " ON DELETE CASCADE " +
                            ")";

            static final String SELECT_BY_CONTACT_ID =
                    "SELECT * " +
                            " FROM " + ContactNumberTable.NAME +
                            " WHERE " + Column.CONTACT_ID + " = ? " +
                            " ORDER BY " + Column.NUMBER +
                            " ASC";

            static final String SELECT_BY_NUMBER =
                    "SELECT * " +
                            " FROM " + ContactNumberTable.NAME +
                            " WHERE (" +
                            Column.TYPE + " = " +  ContactNumber.TYPE_EQUALS + " AND " +
                            " ? = " + Column.NUMBER + ") OR (" +
                            Column.TYPE + " = " +  ContactNumber.TYPE_STARTS + " AND " +
                            " ? LIKE " + Column.NUMBER + "||'%') OR (" +
                            Column.TYPE + " = " +  ContactNumber.TYPE_ENDS + " AND " +
                            " ? LIKE '%'||" + Column.NUMBER + ")";
        }
    }

    // ContactsNumber table item
    public static class ContactNumber {
        public static final int TYPE_EQUALS = 0;
        public static final int TYPE_STARTS = 1;
        public static final int TYPE_ENDS = 2;

        public final long id;
        public final String number;
        public final int type;
        public final long contactId;

        public ContactNumber(long id, @NonNull String number, long contactId) {
            this(id, number, TYPE_EQUALS, contactId);
        }

        public ContactNumber(long id, @NonNull String number, int type, long contactId) {
            this.id = id;
            this.number = number;
            this.type = type;
            this.contactId = contactId;
        }
    }

    // ContactsNumber item cursor wrapper
    public class ContactNumberCursorWrapper extends CursorWrapper {
        private final int ID;
        private final int NUMBER;
        private final int TYPE;
        private final int CONTACT_ID;

        public ContactNumberCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(ContactNumberTable.Column.ID);
            NUMBER = cursor.getColumnIndex(ContactNumberTable.Column.NUMBER);
            TYPE = cursor.getColumnIndex(ContactNumberTable.Column.TYPE);
            CONTACT_ID = cursor.getColumnIndex(ContactNumberTable.Column.CONTACT_ID);
        }

        public ContactNumber getNumber() {
            long id = getLong(ID);
            String number = getString(NUMBER);
            int type = getInt(TYPE);
            long contactId = getLong(CONTACT_ID);
            return new ContactNumber(id, number, type, contactId);
        }
    }

    // Adds number
    public long addNumber(long contactId, @NonNull String number, int type) {
        // try to find existed number for this contact
        ContactNumberCursorWrapper cursor = getNumberByContactId(contactId);
        if(cursor != null) {
            try {
                do {
                    ContactNumber contactNumber = cursor.getNumber();
                    if (contactNumber.type == type &&
                        contactNumber.number.equals(number)) {
                        return contactNumber.id;
                    }
                } while (cursor.moveToNext());
            } finally {
                cursor.close();
            }
        }
        // add new number for contact
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ContactNumberTable.Column.NUMBER, number);
        values.put(ContactNumberTable.Column.TYPE, type);
        values.put(ContactNumberTable.Column.CONTACT_ID, contactId);
        return db.insert(ContactNumberTable.NAME, null, values);
    }

    // Deletes number(s) by contact id
    public int deleteNumberByContractId(long contactId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(ContactNumberTable.NAME,
                ContactNumberTable.Column.CONTACT_ID + " = ?",
                new String[]{String.valueOf(contactId)});
    }

    // Selects number(s) by contact id
    public @Nullable
    ContactNumberCursorWrapper getNumberByContactId(long contactId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactNumberTable.Statement.SELECT_BY_CONTACT_ID,
                new String[]{String.valueOf(contactId)});

        return (validate(cursor) ? new ContactNumberCursorWrapper(cursor) : null);
    }

    // Selects numbers by number value
    public @Nullable
    ContactNumberCursorWrapper getNumberByValue(@NonNull String number) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactNumberTable.Statement.SELECT_BY_NUMBER,
                new String[]{number, number, number});

        return (validate(cursor) ? new ContactNumberCursorWrapper(cursor) : null);
    }

//----------------------------------------------------------------

    // Table of contacts (black/white lists)
    private static class ContactTable {
        static final String NAME = "contact";

        static class Column {
            static final String ID = "_id";
            static final String NAME = "name";
            static final String TYPE = "type"; // black/white type
        }

        static class Statement {
            static final String CREATE =
                    "CREATE TABLE " + ContactTable.NAME +
                            "(" +
                            Column.ID + " INTEGER PRIMARY KEY NOT NULL, " +
                            Column.NAME + " TEXT NOT NULL, " +
                            Column.TYPE + " INTEGER NOT NULL DEFAULT 0 " +
                            ")";

            static final String SELECT_BY_TYPE =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.TYPE + " = ? " +
                            " ORDER BY " + Column.NAME +
                            " ASC";

            static final String SELECT_BY_TYPE_AND_NAME =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.TYPE + " = ? " +
                            " AND " + Column.NAME + " = ? ";

            static final String SELECT_BY_ID =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.ID + " = ? ";

            static final String SELECT_BY_TYPE_FILTER_BY_NAME =
                    "SELECT * " +
                            " FROM " + ContactTable.NAME +
                            " WHERE " + Column.TYPE + " = ? " +
                            " AND " + Column.NAME + " LIKE ? " +
                            " ORDER BY " + Column.NAME +
                            " ASC";
        }
    }

    // Contact cursor wrapper
    public class ContactCursorWrapper extends CursorWrapper implements ContactSource {
        private final int ID;
        private final int NAME;
        private final int TYPE;

        public ContactCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(ContactTable.Column.ID);
            NAME = getColumnIndex(ContactTable.Column.NAME);
            TYPE = getColumnIndex(ContactTable.Column.TYPE);
        }

        @Override
        public Contact getContact() {
            return getContact(true);
        }

        public Contact getContact(boolean withNumbers) {
            long id = getLong(ID);
            String name = getString(NAME);
            int type = getInt(TYPE);

            List<ContactNumber> numbers = new LinkedList<>();
            if(withNumbers) {
                ContactNumberCursorWrapper cursor = getNumberByContactId(id);
                if(cursor != null) {
                    do {
                        numbers.add(cursor.getNumber());
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }

            return new Contact(id, name, type, numbers);
        }
    }

    // Selects all contacts by type
    public @Nullable ContactCursorWrapper getContacts(int type) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_TYPE,
                new String[]{String.valueOf(type)});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Selects all contacts by type filtering by passed filter
    public @Nullable ContactCursorWrapper getContacts(int type, @Nullable String filter) {
        if(filter == null) {
            return getContacts(type);
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_TYPE_FILTER_BY_NAME,
                new String[]{String.valueOf(type), "%" + filter + "%"});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Selects all contacts by type and name
    public @Nullable ContactCursorWrapper getContact(int type, String name) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_TYPE_AND_NAME,
                new String[]{String.valueOf(type), name});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Selects contact by id
    public @Nullable ContactCursorWrapper getContact(long contactId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                ContactTable.Statement.SELECT_BY_ID,
                new String[]{String.valueOf(contactId)});

        return (validate(cursor) ? new ContactCursorWrapper(cursor) : null);
    }

    // Adds contact
    private long addContact(@NonNull String name, int type) {
        // try to find existed contact by name
        ContactCursorWrapper cursor = getContact(type, name);
        if(cursor != null) {
            Contact contact = cursor.getContact(false);
            cursor.close();
            return contact.id;
        }
        // add new contact
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ContactTable.Column.NAME, name);
        values.put(ContactTable.Column.TYPE, type);
        return db.insert(ContactTable.NAME, null, values);
    }

    // Adds contact
    public long addContact(@NonNull String name, int type, @NonNull List<ContactNumber> numbers) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        long contactId;
        try {
            contactId = addContact(name, type);
            if(contactId >= 0) {
                for (ContactNumber number : numbers) {
                    if (addNumber(contactId, number.number, number.type) == -1) {
                        return -1;
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return contactId;
    }

    // Deletes all contacts specified in container with specified type
    public int deleteContacts(int type, IdentifiersContainer contactIds, @Nullable  String filter) {
        if(contactIds.isEmpty()) return 0;

        boolean all = contactIds.isFull();
        List<String> ids = contactIds.getIdentifiers(new LinkedList<String>());

        // build 'WHERE' clause
        String clause = Common.concatClauses(new String[] {
            ContactTable.Column.TYPE  + " = " + type,
            Common.getLikeClause(ContactTable.Column.NAME, filter),
            Common.getInClause(ContactTable.Column.ID, all, ids)
        });

        // delete contacts
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(ContactTable.NAME, clause, null);
    }

    // Deletes contact by id
    public int deleteContact(long contactId) {
        String clause = ContactTable.Column.ID  + " = " + contactId;
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(ContactTable.NAME, clause, null);
    }

//----------------------------------------------------------------

    // The contact
    public static class Contact {
        public static final int TYPE_BLACK_LIST = 1;
        public static final int TYPE_WHITE_LIST = 2;

        public final long id;
        public final String name;
        public final int type;
        public final List<ContactNumber> numbers;

        Contact(long id, @NonNull String name, int type, @NonNull List<ContactNumber> numbers) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.numbers = numbers;
        }
    }

    // Source of the contact
    public interface ContactSource {
        Contact getContact();
    }

//----------------------------------------------------------------

    // Returns contact numbers
    private List<ContactNumber> getContactNumbers(@NonNull String number) {
        List<ContactNumber> list = new LinkedList<>();
        ContactNumberCursorWrapper cursor = getNumberByValue(number);
        if(cursor != null) {
            do {
                list.add(cursor.getNumber());
            } while (cursor.moveToNext());
            cursor.close();
        }

        return list;
    }

    // Returns contacts by contact numbers
    private List<Contact> getContacts(@NonNull List<ContactNumber> numbers) {
        List<Contact> list = new LinkedList<>();
        for(ContactNumber number : numbers) {
            DatabaseAccessHelper.ContactCursorWrapper cursor = getContact(number.contactId);
            if(cursor != null) {
                list.add(cursor.getContact(false));
                cursor.close();
            }
        }

        return list;
    }

    // Returns contacts by contact number
    public List<Contact> getContacts(@NonNull String number) {
        List<ContactNumber> numbers = getContactNumbers(number);
        return getContacts(numbers);
    }

//----------------------------------------------------------------

    // Table of settings
    private static class SettingsTable {
        static final String NAME = "settings";

        static class Column {
            static final String ID = "_id";
            static final String NAME = "name";
            static final String VALUE = "value";
        }

        static class Statement {
            static final String CREATE =
                    "CREATE TABLE " + SettingsTable.NAME +
                            "(" +
                            Column.ID + " INTEGER PRIMARY KEY NOT NULL, " +
                            Column.NAME + " TEXT NOT NULL, " +
                            Column.VALUE + " TEXT " +
                            ")";

            static final String SELECT_BY_NAME =
                    "SELECT * " +
                            " FROM " + SettingsTable.NAME +
                            " WHERE " + Column.NAME + " = ? ";
        }
    }

    // Settings item
    public class SettingsItem {
        public final long id;
        public final String name;
        public final String value;

        public SettingsItem(long id, String name, String value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }
    }

    // SettingsItem cursor wrapper
    public class SettingsItemCursorWrapper extends CursorWrapper {
        private final int ID;
        private final int NAME;
        private final int VALUE;

        public SettingsItemCursorWrapper(Cursor cursor) {
            super(cursor);
            cursor.moveToFirst();
            ID = cursor.getColumnIndex(SettingsTable.Column.ID);
            NAME = cursor.getColumnIndex(SettingsTable.Column.NAME);
            VALUE = cursor.getColumnIndex(SettingsTable.Column.VALUE);
        }

        public SettingsItem getSettings() {
            long id = getLong(ID);
            String name = getString(NAME);
            String value = getString(VALUE);
            return new SettingsItem(id, name, value);
        }
    }

    // Selects settings by name
    private @Nullable SettingsItemCursorWrapper getSettings(@NonNull String name) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                SettingsTable.Statement.SELECT_BY_NAME,
                new String[]{name});

        return (validate(cursor) ? new SettingsItemCursorWrapper(cursor) : null);
    }

    // Selects value of settings by name
    public @Nullable String getSettingsValue(@NonNull String name) {
        SettingsItemCursorWrapper cursor = getSettings(name);
        if(cursor != null) {
            SettingsItem item = cursor.getSettings();
            cursor.close();
            return item.value;
        }
        return null;
    }

    // Sets value of settings with specified name
    public boolean setSettingsValue(@NonNull String name, @NonNull String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SettingsTable.Column.VALUE, value);
        // try to update value
        int n = db.update(SettingsTable.NAME,
                values,
                SettingsTable.Column.NAME + " = ? ",
                new String[]{name});
        if(n == 0) {
            // try to add name/value
            values.put(SettingsTable.Column.NAME, name);
            return db.insert(SettingsTable.NAME, null, values) >= 0;
        }

        return true;
    }

//----------------------------------------------------------------

/*
    // TODO temporary
    static void firstInit(Context context) {
        DatabaseAccessHelper dao = DatabaseAccessHelper.getInstance(context);
        InfoStorage storage = InfoStorage.getInstance();
        // writing from xml to files
        //storage.firstInit();
        List<InfoStorage.SmsInfo> itemList =
                storage.loadSmsInfoList(new ArrayList<InfoStorage.SmsInfo>());
        for(InfoStorage.SmsInfo info : itemList) {
            dao.addJournalRecord(info.time, info.sender, info.text);
        }
    }
*/
}