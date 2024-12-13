package artisticendeavors.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import coil.load
import artisticendeavors.databinding.ActivityFullSizeImageBinding

class FullSizeImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullSizeImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullSizeImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra("image_url")
        binding.ivFullSizeImage.load(imageUrl)
    }
}