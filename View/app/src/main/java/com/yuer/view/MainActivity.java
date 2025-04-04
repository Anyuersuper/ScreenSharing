package com.yuer.view;





import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity{

    private EditText editText;
    private String lastInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取编辑框和按钮的引用
        editText = findViewById(R.id.editText);
        Button showButton = findViewById(R.id.showButton);
        
        // 如果有保存的状态，恢复它
        if (savedInstanceState != null) {
            lastInput = savedInstanceState.getString("lastInput", "");
            editText.setText(lastInput);
        }

        // 设置按钮点击事件
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取编辑框的内容
                String content = editText.getText().toString();
                
                // 检查输入是否为空
                if (content.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请输入IP地址和端口", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 保存当前输入
                lastInput = content;

                // 创建意图并传递URL参数
                Intent intent = new Intent(MainActivity.this, SecondMain.class);
                intent.putExtra("url", content);
                startActivity(intent);  // 启动 SecondMain 活动
            }
        });
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 保存当前输入，以便在屏幕旋转或配置变化时恢复
        if (editText != null) {
            outState.putString("lastInput", editText.getText().toString());
        } else if (lastInput != null) {
            outState.putString("lastInput", lastInput);
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 屏幕方向改变时触发的回调，可以在这里处理UI调整
    }
}