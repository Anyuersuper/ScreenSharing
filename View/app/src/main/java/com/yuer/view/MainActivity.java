package com.yuer.view;





import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取编辑框和按钮的引用
        EditText editText = findViewById(R.id.editText);
        Button showButton = findViewById(R.id.showButton);

        // 设置按钮点击事件
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取编辑框的内容
                String content = editText.getText().toString();

                Intent intent = new Intent(MainActivity.this, SecondMain.class);
                intent.putExtra("url",content);
                startActivity(intent);  // 启动 SecondMain 活动

            }
        });
    }
}