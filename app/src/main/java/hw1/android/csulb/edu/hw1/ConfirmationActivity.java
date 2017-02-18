package hw1.android.csulb.edu.hw1;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static hw1.android.csulb.edu.hw1.Extras.Constants.COUNTRY_CODE;
import static hw1.android.csulb.edu.hw1.Extras.Constants.RESTAURANT_NUMBER;
import static hw1.android.csulb.edu.hw1.Extras.Constants.XML_FILEPATH;

public class ConfirmationActivity extends AppCompatActivity {


    String contact_number;
    ListView listView;
    LinkedHashSet itemSet;
    Double totalAmount = 0.0;
    TextView totalAmountText;
    boolean smsSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        contact_number = COUNTRY_CODE+RESTAURANT_NUMBER;
        itemSet = (LinkedHashSet) getIntent().getSerializableExtra("itemSet");
        readXML();

        listView = (ListView) findViewById(R.id.listViewItem);
        CustomAdapter customAdapter = new CustomAdapter(this, itemSet);
        listView.setAdapter(customAdapter);
        totalAmountText = (TextView) findViewById(R.id.totalAmount);
        String totalAmountBtnText = "Total amount: $ " + String.valueOf(truncAmount(totalAmount));
        totalAmountText.setText(totalAmountBtnText);
    }

    private void addToTotal(Double amount) {
        totalAmount += amount;
    }

    private String getValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = nodeList.item(0);
        return node.getNodeValue();
    }

    private void readXML() {
        try {
            InputStream file = getAssets().open(XML_FILEPATH);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            Document doc = dbFactory.newDocumentBuilder().parse(file);

            Element element=doc.getDocumentElement();
            element.normalize();
            NodeList nList = doc.getElementsByTagName("item");
            for (int i=0; i<nList.getLength(); i++) {
                Node node = nList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    addValueFromNode(node);
                }
            }//end of for loop
            Log.d("Total Amount", String.valueOf(totalAmount));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private void addValueFromNode(Node node) {
        Element element2 = (Element) node;
        String name = getValue("name",element2);

        for(Iterator<Item> it = itemSet.iterator(); it.hasNext(); ) {
            Item item = it.next();
            if(item.getItem_name().equals(name)) {
                String amount = getValue("price",element2);
                item.setItem_price(amount);
                addToTotal(Double.valueOf(amount));
                Log.d("Selected Item Name: ", item.getItem_name());
                Log.d("Selected Item Amount: ", String.valueOf(item.getItem_price()));
                addToTotal(Double.valueOf(amount));
            }
        }
    }

    public void sendSMS(View view) {
        if (smsSent) {
            Toast.makeText(getApplicationContext(), "Your order has already been placed", Toast.LENGTH_LONG).show();
            return;
        }
        String msgContent = "";
        for(Iterator<Item> it = itemSet.iterator(); it.hasNext(); ) {
            Item item = it.next();
            msgContent += item.getItem_name()+" Price: "+item.getItem_price() + "\n";
        }

        msgContent += "---------" + "\n";
        msgContent += "Total: " + String.valueOf(truncAmount(totalAmount));

        System.out.println(msgContent);
        SmsManager smsMgr = SmsManager.getDefault();
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},1);

        try {
                if (msgContent.length() > 160) {
                    ArrayList<String> Parts = smsMgr.divideMessage(msgContent);
                    smsMgr.sendMultipartTextMessage(contact_number, null, Parts, null, null);
                    smsSent = true;
                } else {
                    smsMgr.sendTextMessage(contact_number, null, msgContent, null, null);
                    smsSent = true;
                    Toast.makeText(getApplicationContext(), "Your order has been placed via SMS", Toast.LENGTH_LONG).show();
                }
            }
        catch (Exception e) {
            smsSent = false;
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "SMS couldn't be sent.", Toast.LENGTH_LONG).show();
        }
    }

    @NonNull
    private Double truncAmount(Double totalAmount) {
        return BigDecimal.valueOf(totalAmount).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
