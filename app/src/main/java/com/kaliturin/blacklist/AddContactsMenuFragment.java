package com.kaliturin.blacklist;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kaliturin.blacklist.ContactsAccessHelper.ContactSourceType;


/**
 * Fragment for representation the menu of the contacts sources
 * for choosing where add contact from
 */
public class AddContactsMenuFragment extends Fragment {
    // bundle argument name
    public static final String CONTACT_TYPE = "CONTACT_TYPE";
    private int contactType = 0;

    // On menu items click listener
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ContactSourceType sourceType = null;
            String title = "";
            switch (v.getId()) {
                case R.id.add_from_contacts:
                    title = getString(R.string.contacts_list);
                    sourceType = ContactSourceType.FROM_CONTACTS;
                    break;
                case R.id.add_from_calls:
                    title = getString(R.string.calls_list);
                    sourceType = ContactSourceType.FROM_CALLS_LOG;
                    break;
                case R.id.add_from_sms:
                    title = getString(R.string.sms_inbox_list);
                    sourceType = ContactSourceType.FROM_SMS_INBOX;
                    break;
                case R.id.add_manually:
                    title = getString(R.string.adding_contact);
                    break;
            }

            Bundle arguments = new Bundle();
            Fragment fragment;
            if(sourceType != null) {
                // create fragment of adding contacts from inbox/calls
                arguments.putInt(AddContactsFragment.CONTACT_TYPE, contactType);
                arguments.putSerializable(AddContactsFragment.SOURCE_TYPE, sourceType);
                fragment = new AddContactsFragment();
            } else {
                // create fragment of adding contacts manually
                arguments.putInt(AddOrEditContactFragment.CONTACT_TYPE, contactType);
                fragment = new AddOrEditContactFragment();
            }

            // add fragment args
            fragment.setArguments(arguments);
            // open the dialog activity with the fragment of contact adding
            DialogActivity.show(getActivity(), fragment, title, 0);
        }
    };

    public AddContactsMenuFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        contactType = bundle.getInt(CONTACT_TYPE, 0);

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_contacts_menu, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // add on items click listener
        view.findViewById(R.id.add_from_contacts).setOnClickListener(onClickListener);
        view.findViewById(R.id.add_from_calls).setOnClickListener(onClickListener);
        view.findViewById(R.id.add_from_sms).setOnClickListener(onClickListener);
        view.findViewById(R.id.add_manually).setOnClickListener(onClickListener);
    }
}