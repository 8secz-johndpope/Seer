 package com.thoughtworks.pumpkin.fragment;
 
 import android.app.AlertDialog;
 import android.app.ProgressDialog;
 import android.content.DialogInterface;
 import android.content.Intent;
 import android.os.Bundle;
 import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
 import android.view.View;
 import android.view.ViewGroup;
 import android.widget.AdapterView;
 import android.widget.GridView;
 import android.widget.Spinner;
 import android.widget.TextView;
 import com.parse.FindCallback;
 import com.parse.ParseException;
 import com.parse.ParseObject;
 import com.parse.ParseQuery;
 import com.thoughtworks.pumpkin.BaseActivity;
 import com.thoughtworks.pumpkin.R;
 import com.thoughtworks.pumpkin.ShopActivity;
 import com.thoughtworks.pumpkin.adapter.BooksAdapter;
 import com.thoughtworks.pumpkin.adapter.ShopsAdapter;
 import com.thoughtworks.pumpkin.helper.Constant;
 import com.thoughtworks.pumpkin.helper.PumpkinDB;
 import com.thoughtworks.pumpkin.helper.Util;
import roboguice.fragment.RoboFragment;
 
 import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
public class ViewBooks extends RoboFragment {
 
     private GridView booksGridView;
     private String category;
     private String wishList;
 
     @Override
     public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
         setHasOptionsMenu(true);
         return inflater.inflate(R.layout.books, container, false);
     }
 
     @Override
     public void onViewCreated(View view, Bundle savedInstanceState) {
         category = getActivity().getIntent().getStringExtra("category");
         wishList = getActivity().getIntent().getStringExtra("wishlist");
         booksGridView = (GridView) view.findViewById(R.id.books);
         ((BaseActivity)getActivity()).getSupportActionBar().setTitle((category != null) ? category : wishList);
         if (category != null) findByCategory(category);
         if (wishList != null) findByWishList(wishList);
     }
 
     private void findByWishList(String wishList) {
         final ProgressDialog dialog = Util.showProgressDialog(getActivity());
         ParseQuery query = new ParseQuery(Constant.ParseObject.WISH_LIST_BOOK);
         ParseQuery innerQuery = new ParseQuery(Constant.ParseObject.WISH_LIST);
         innerQuery.whereEqualTo(Constant.ParseObject.COLUMN.WISH_LIST.NAME, wishList);
         query.whereMatchesQuery(Constant.ParseObject.COLUMN.WISH_LIST_BOOK.WISH_LIST, innerQuery);
         query.findInBackground(new FindCallback() {
             @Override
             public void done(List<ParseObject> wishListBooks, ParseException e) {
                 if (wishListBooks.isEmpty()) {
                     AlertDialog alertDialog = Util.dialog("No books in this list.", getActivity());
                     alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                         @Override
                         public void onDismiss(DialogInterface dialogInterface) {
                             if (dialog.isShowing()) dialog.dismiss();
                             getActivity().finish();
                         }
                     });
                     alertDialog.show();
                 } else {
                     List<ParseObject> books = new ArrayList<ParseObject>();
                     for (ParseObject wishListBook : wishListBooks) {
                         books.add(wishListBook.getParseObject(Constant.ParseObject.COLUMN.WISH_LIST_BOOK.BOOK));
                     }
                     loadBooks(books, false, dialog);
                 }
             }
         });
     }
 
     private void findByCategory(String category) {
         ParseQuery query = new ParseQuery(Constant.ParseObject.BOOK);
         ParseQuery innerQuery = new ParseQuery(Constant.ParseObject.CATEGORY);
         innerQuery.whereEqualTo(Constant.ParseObject.COLUMN.BOOK.NAME, category);
         query.whereMatchesQuery("parent", innerQuery);
         query.orderByAscending(Constant.ParseObject.COLUMN.BOOK.RATING);
         final ProgressDialog dialog = Util.showProgressDialog(getActivity());
         query.findInBackground(new FindCallback() {
             @Override
             public void done(List<ParseObject> parseObjects, ParseException e) {
                 loadBooks(parseObjects, true, dialog);
             }
         });
     }
 
     private void loadBooks(List<ParseObject> books, boolean fetchAll, final ProgressDialog dialog) {
         ArrayList<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
         for (ParseObject book : books) {
             HashMap<String, Object> map = new HashMap<String, Object>();
             map.put("book", book);
             map.put("complete", fetchAll);
             maps.add(map);
         }
         if (dialog.isShowing()) dialog.dismiss();
         booksGridView.setAdapter(new BooksAdapter(getActivity(), maps, R.layout.book,
                 new String[]{"bookImage", "title", "rank", "objectId"}, new int[]{R.id.bookImage, R.id.title, R.id.rank, R.id.heart}));
     }
 
     @Override
     public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
         inflater.inflate(R.menu.view_cart, menu);
         MenuItem menuItem = menu.findItem(R.id.menu_cart);
         Spinner spinner = (Spinner) menuItem.getActionView();
         List<String> shops = new PumpkinDB(getActivity()).getShops();
         shops.add(0, getResources().getString(R.string.view_cart));
         spinner.setAdapter(new ShopsAdapter<String>(getActivity(), R.layout.spin_shop_rowitem, shops));
         spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
             @Override
             public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                 if (i == 0) return;
                 Intent intent = new Intent(getActivity(), ShopActivity.class);
                 if (category != null) intent.putExtra("category", category);
                 if (wishList != null) intent.putExtra("wishlist", wishList);
                 intent.putExtra("shop", ((TextView) view.findViewById(R.id.text1)).getText());
                 startActivity(intent);
             }
 
             @Override
             public void onNothingSelected(AdapterView<?> adapterView) {
             }
         });
     }
 }
